package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.IOUtil;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


public class PredictionBoltJPMML implements IRichBolt {

  private static final Logger LOG = Logger.getLogger(PredictionBolt.class);
  private String phoenixDriverPath;
  private Driver phoenixDriver;
  private Properties topologyConfig;
  private OutputCollector collector;
  private Evaluator modelEvaluator;


  public PredictionBoltJPMML(Properties topologyConfig) {
    this.topologyConfig = topologyConfig;
  }


  public void prepare(Map stormConf, TopologyContext context,
                      OutputCollector collector) {
    this.collector = collector;
    modelEvaluator = instantiateJPMMLModel();
  }


  public void execute(Tuple input) {
    String eventType = input.getStringByField("eventType");
    boolean violationPredicted;
    LOG.info("Entered prediction bolt execute (JPMML Version)...");
    if (eventType.equals("Normal")) {
      double[] predictionParams = enrichEvent(input);
      //run the input through the model and predict violation
      violationPredicted = evaluateJPMMLModel(modelEvaluator, predictionParams);
      LOG.info("Prediction is: " + violationPredicted);

      String driverName = input.getStringByField("driverName");
      String routeName = input.getStringByField("routeName");
      int truckId = input.getIntegerByField("truckId");
      Timestamp eventTime = (Timestamp) input.getValueByField("eventTime");
      double longitude = input.getDoubleByField("longitude");
      double latitude = input.getDoubleByField("latitude");
      double driverId = input.getIntegerByField("driverId");
      SimpleDateFormat sdf = new SimpleDateFormat();

      collector.emit(input, new Values(
          violationPredicted == false ? "normal" : "violation",
          driverName,
          routeName,
          driverId,
          truckId,
          sdf.format(new Date(eventTime.getTime())),
          longitude,
          latitude,
          predictionParams[0] == 1 ? "Y" : "N", // driver certification status
          predictionParams[1] == 1 ? "miles" : "hourly", // driver wage plan
          predictionParams[2] * 100,  // hours feature was scaled down by 100
          predictionParams[3] * 1000, // miles feature was scaled down by 1000
          predictionParams[4] == 1 ? "Y" : "N", // foggy weather
          predictionParams[5] == 1 ? "Y" : "N", // rainy weather
          predictionParams[6] == 1 ? "Y" : "N"  // windy weather
      ));

      if (violationPredicted) {
        try {
          writePredictionToHDFS(input, predictionParams, violationPredicted);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("Couldn't write prediction to hdfs" + e);
        }
      }
    }

    // TODO: don't ack if there's an error
    collector.ack(input);
  }


  public int getWeek(Tuple input) {
    Timestamp ts = (Timestamp) input.getValueByField("eventTime");
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(new Date(ts.getTime()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cal.get(Calendar.WEEK_OF_YEAR);
  }


  public double[] enrichEvent(Tuple input) {
    double driverID = input.getIntegerByField("driverId");
    Connection conn = null;
    try {
      conn = DriverManager.getConnection("jdbc:phoenix:" + topologyConfig.getProperty("hbase.zookeeper.server") +
          ":2181:/hbase-unsecure");
      // input features to spark model
      double certified = 0, wageplan = 0, hours_logged = 0, miles_logged = 0, foggy = 0, rainy = 0, windy = 0;
      // get driver certification status and wage plan from hbase
      ResultSet rst = conn.createStatement().
          executeQuery("select certified, wage_plan from drivers where driverid=" + driverID);
      while (rst.next()) {
        certified = rst.getString(1).equals("Y") ? 1 : 0;
        wageplan = rst.getString(1).equals("miles") ? 1 : 0;
      }
      // get driver fatigue status from timesheet table in hbase
      rst = conn.createStatement().
          executeQuery("select hours_logged, miles_logged from timesheet"
              + " where driverid=" + driverID + " and week=" + getWeek(input));
      while (rst.next()) {
        hours_logged = rst.getInt(1);
        miles_logged = rst.getInt(2);
      }
      System.out.println("HOURS LOGGED " + hours_logged);
      System.out.println("MILES LOGGED " + miles_logged);
      // scale the hours & miles features for spark model
      hours_logged = hours_logged / 100;
      miles_logged = miles_logged / 1000;
      // get weather conditions - currently these are being simulated randomly, with a bias
      // towards more fog for dangerous drivers
      if (driverID == 12) // jamie
        foggy = new Random().nextInt(100) < 50 ? 1 : 0;
      else if (driverID == 11) // george
        foggy = new Random().nextInt(100) < 35 ? 1 : 0;
      else
        foggy = new Random().nextInt(100) < 12 ? 1 : 0;

      rainy = new Random().nextInt(100) < 20 ? 1 : 0;
      windy = new Random().nextInt(100) < 30 ? 1 : 0;

      // return the enriched event
      return new double[]{certified, wageplan, hours_logged, miles_logged, foggy, rainy, windy};

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);

    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  private Evaluator instantiateJPMMLModel() {
    Configuration conf = new Configuration();
    conf.set("fs.defaultFS", topologyConfig.getProperty("hdfs.url"));
    InputStream pmmlFile = null;
    try {
      pmmlFile = getPmmlModelFromHDFS(new Path(topologyConfig.getProperty("hdfs.url") +
          "/tmp/pmml/TruckDemoModel-pmml.xml"), conf);
    } catch (Exception e) {
      LOG.error("Couldn't instantiate JPMML model in prediction bolt: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    PMML pmml = null;
    try {
      pmml = IOUtil.unmarshal(pmmlFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    PMMLManager pmmlManager = new PMMLManager(pmml);
    // Load the default model
    Evaluator evaluator = (Evaluator) pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
    return evaluator;
  }


  /*
  * JPMML helper methods
  * */
  private boolean evaluateJPMMLModel(Evaluator evaluator, double[] inputValues) {
    boolean prediction = false;
    Map<FieldName, ?> arguments = prepareArguments(evaluator, inputValues);
    Map<FieldName, ?> result = evaluator.evaluate(arguments);
    ArrayList<FieldName> predictedFields = new ArrayList<FieldName>(evaluator.getPredictedFields());
    FieldName predictedField = predictedFields.get(0); // we are predicting only one field here
    //DataField dataField = evaluator.getDataField(predictedField); //only need for debug output
    Object predictedValue = result.get(predictedField);
    if (EvaluatorUtil.decode(predictedValue).equals("yes")) {
      prediction = true;
    }
    return prediction;
  }

  public Map<FieldName, ?> prepareArguments(Evaluator evaluator, double[] inputValues) {
    Map<FieldName, Object> arguments = new LinkedHashMap<FieldName, Object>();
    List<FieldName> activeFields = evaluator.getActiveFields();
    // loop over and prepare each argument
    int idx = 0;
    for (FieldName activeField : activeFields) {
      DataField dataField = evaluator.getDataField(activeField);
      arguments.put(activeField, evaluator.prepare(activeField, inputValues[idx]));
      idx++;
    }
    return arguments;
  }

  private InputStream getPmmlModelFromHDFS(Path location, Configuration conf) throws Exception {
    FileSystem fileSystem = FileSystem.get(location.toUri(), conf);
    FileStatus[] files = fileSystem.listStatus(location);
    if (files == null)
      throw new Exception("Couldn't find PMML file at: " + location);
    InputStream stream = fileSystem.open(files[0].getPath());
    return stream;
  }


  public void writePredictionToHDFS(Tuple input, double[] params, boolean prediction) throws Exception {
    try {
      Configuration conf = new Configuration();
      conf.set("fs.defaultFS", topologyConfig.getProperty("hdfs.url"));
      FileSystem fs = FileSystem.get(conf);
      Path pt = new Path(topologyConfig.getProperty("hdfs.url") + "/tmp/predictions/" + System.currentTimeMillis());
      BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs.create(pt, true)));
      br.write("Original Event: " + input + "\n");
      br.write("\n");
      br.write("Certification status (from HBase): " + (params[0] == 1 ? "Y" : "N") + "\n");
      br.write("Wage plan (from HBase): " + (params[1] == 1 ? "Miles" : "Hours" + "\n"));
      br.write("Hours logged (from HBase): " + params[2] * 100 + "\n");
      br.write("Miles logged (from HBase): " + params[3] * 1000 + "\n");
      br.write("\n");
      br.write("Is Foggy? (from weather API): " + (params[4] == 1 ? "Y" : "N" + "\n"));
      br.write("Is Rainy? (from weather API): " + (params[5] == 1 ? "Y" : "N" + "\n"));
      br.write("Is Windy? (from weather API): " + (params[6] == 1 ? "Y" : "N" + "\n"));
      br.write("\n");
      br.write("\n");
      br.write("Input to JPMML model: " + Arrays.toString(params) + "\n");
      br.write("\n");
      br.write("Prediction from JPMML model: " + prediction + "\n");
      br.flush();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * We don't need to set any configuration because at deployment time, it should pick up all configuration from
   * hbase-site.xml
   * as long as it in classpath. Note that we store hbase-site.xml in src/main/resources so it will be in the
   * topology jar that gets deployed
   */

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("prediction", "driverName",
        "routeName", "driverId", "truckId", "timeStamp",
        "longitude", "latitude", "certified",
        "wagePlan", "hours_logged", "miles_logged",
        "isFoggy", "isRainy", "isWindy"));
  }

  public Map<String, Object> getComponentConfiguration() {
    return null;
  }

  public void cleanup() {

  }
}




