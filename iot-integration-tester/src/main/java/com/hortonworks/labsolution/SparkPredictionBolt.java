package com.hortonworks.labsolution;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.IOUtil;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import sun.misc.Launcher;

import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class SparkPredictionBolt implements IRichBolt {

  private static final Logger LOG = Logger.getLogger(SparkPredictionBolt.class);
  private Evaluator modelEvaluator;
  private OutputCollector collector;
  private static final Map<Integer, _driverRow> driverTable = new HashMap<>();
  private static final Map<Integer, Map<Integer, _weekData>> timesheetTable = new HashMap<>();

  @Override
  public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
    synchronized (this) {
      buildDriverRecords();
      modelEvaluator = instantiateJPMMLModel();
      collector = outputCollector;
    }
  }

  @Override
  public void execute(Tuple input) {
    String eventType = input.getStringByField("eventType");
    boolean violationPredicted;
    if (eventType.equals("Normal")) {
      double[] predictionParams = enrichEvent(input);
      //run the input through the model and predict violation
      violationPredicted = evaluateJPMMLModel(modelEvaluator, predictionParams);

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

      String st = violationPredicted ? "!! ALERT: Infraction" : "Normal" ;
      System.out.print("\n\n\nPrediction for " + driverName + ", driving from " + routeName + " is: ");
      System.out.println(st);
      System.out.println("Hours Driven = " + Math.round(predictionParams[2]) * 100);
      System.out.println("Miles Driven = " + Math.round(predictionParams[3]) * 1000);
      StringBuilder sb = new StringBuilder();
      sb.append(predictionParams[4] == 1 ? "Foggy" : "Not Foggy");
      sb.append(", ");
      sb.append(predictionParams[5] == 1 ? "Rainy" : "Not Rainy");
      sb.append(", ");
      sb.append(predictionParams[6] == 1 ? "Windy" : "Not Windy");
      System.out.println("Weather conditions: " + sb.toString());
      LOG.info(st + " predicted for " + driverName + " along route " + routeName);
    }
  }

  @Override
  public void cleanup() {

  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
    outputFieldsDeclarer.declare(new Fields("prediction", "driverName",
        "routeName", "driverId", "truckId", "timeStamp",
        "longitude", "latitude", "certified",
        "wagePlan", "hours_logged", "miles_logged",
        "isFoggy", "isRainy", "isWindy"));
  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    return null;
  }

  private void buildDriverRecords() {
    CSVParser driverParser, timesheetParser;
    try {
      URL driverURL = Launcher.class.getResource("/drivers.csv").toURI().toURL();
      URL timesheetURL = Launcher.class.getResource("/timesheet.csv").toURI().toURL();
      driverParser = CSVParser.parse(driverURL, Charset.defaultCharset(), CSVFormat.RFC4180);
      timesheetParser = CSVParser.parse(timesheetURL, Charset.defaultCharset(), CSVFormat.RFC4180);
      for (CSVRecord d : driverParser) {
        int driverId = Integer.parseInt(d.get(0));
        String name = d.get(1);
        int isCertified = d.get(2).equals("Y") ? 1 : 0;
        String wagePlan = d.get(3);
        driverTable.put(driverId,
            new _driverRow(driverId, name, isCertified, wagePlan));
      }
      for (CSVRecord t : timesheetParser) {
        int driverId = Integer.parseInt(t.get(0));
        int weekNum = Integer.parseInt(t.get(1));
        int hrsLoggedThisWeek = Integer.parseInt(t.get(2));
        int milesLoggedThisWeek = Integer.parseInt(t.get(3));
        _weekData weekRow = new _weekData(weekNum, hrsLoggedThisWeek, milesLoggedThisWeek);
        if (timesheetTable.containsKey(driverId)) {
          Map existingWeeklyData = timesheetTable.get(driverId);
          existingWeeklyData.put(weekNum, weekRow);
        } else {
          timesheetTable.put(driverId, new HashMap<Integer, _weekData>());
          timesheetTable.get(driverId).put(weekNum, weekRow);
        }
        // miles logged this week
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  private void printTables() {
    System.out.println("\n\n\ndriverTable.entrySet().size() = " + driverTable.entrySet().size());
    for (Map.Entry<Integer, _driverRow> integer_driverRowEntry : driverTable.entrySet()) {
      int driverid = integer_driverRowEntry.getKey();
      _driverRow dr = integer_driverRowEntry.getValue();
      System.out.println("driver id = " + driverid);
      System.out.println("driver name = " + dr.name);
    }
  }

  private double[] enrichEvent(Tuple input) {
    int driverID = input.getIntegerByField("driverId");
    double certified = 0, wageplan = 0, hours_logged = 0, miles_logged = 0, foggy = 0, rainy = 0, windy = 0;
    // get driver certification status and wage plan from driverTable
    certified = driverTable.get(driverID).isCertified;
    wageplan = driverTable.get(driverID).wagePlan.equals("miles") ? 1 : 0;
    // get fatigue info : miles and hrs from weekly data
    int weekNum = getWeek(input);
    hours_logged = timesheetTable.get(driverID).get(weekNum).hoursLogged;
    miles_logged = timesheetTable.get(driverID).get(weekNum).milesLogged;
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
  }


  private int getWeek(Tuple input) {
    Timestamp ts = (Timestamp) input.getValueByField("eventTime");
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(new Date(ts.getTime()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cal.get(Calendar.WEEK_OF_YEAR);
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

  private Map<FieldName, ?> prepareArguments(Evaluator evaluator, double[] inputValues) {
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

  private Evaluator instantiateJPMMLModel() {
    PMML pmml = null;
    try {
      pmml = IOUtil.unmarshal(Launcher.class.getResource("/predictionModel-PMML.xml").openStream());
    } catch (Exception e) {
      e.printStackTrace();
    }
    PMMLManager pmmlManager = new PMMLManager(pmml);
    Evaluator evaluator = (Evaluator) pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
    return evaluator;
  }


  private class _driverRow implements Serializable {
    public _driverRow(int driverId, String name, int isCertified, String wagePlan) {
      this.driverId = driverId;
      this.name = name;
      this.isCertified = isCertified;
      this.wagePlan = wagePlan;
    }

    int driverId;
    String name;
    int isCertified;
    String wagePlan;
  }

  private class _weekData implements Serializable {
    public _weekData(int week, int hoursLogged, int milesLogged) {
      this.week = week;
      this.hoursLogged = hoursLogged;
      this.milesLogged = milesLogged;
    }

    int week;
    int hoursLogged;
    int milesLogged;
  }

}


