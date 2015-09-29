package com.hortonworks;

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

/**
 * In this Lab you will add prediction to the event stream by applying a Linear Regression Model to each event.
 * The model was built using Spark ML and is present as a PMML file in the src/main/resources folder.
 * Your job is to enrich each original event by adding context specific info, and then passing in the enriched event
 * to the model for predicting whether there's going to be a traffic violation or not.
 *
 * There are two parts for this lab:
 *          - Lab Exercise 1: You'll parse driver and timesheer csv files present in the resource folder and populate
 *          some maps
 *          - Lab Exercise 2: Yuu'll use the maps to enrich each event, and apply the JPMML model, predict and emit
 *          the prediction with the event
 */

public class SparkPredictionBoltLab implements IRichBolt {

  private static final Logger LOG = Logger.getLogger(SparkPredictionBoltLab.class);
  private Evaluator modelEvaluator;
  private OutputCollector collector;

  // in - memory data structures to simulate event enrichment - you'll populate them with Lab Exercise 1
  // normally this info will be loaded in Hive or Hbase from existing ERP and CRM systems
  private static final Map<Integer, _driverRow> driverTable = new HashMap<>();
  private static final Map<Integer, Map<Integer, _weekData>> timesheetTable = new HashMap<>();

  @Override
  public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
    synchronized (this) {
      // Lab Exercise 1: Implement the buildDriverRecords() method to populate the driverTable and timesheetTable maps
      buildDriverRecords();
      modelEvaluator = instantiateJPMMLModel();
      collector = outputCollector;
    }
  }

  @Override
  public void execute(Tuple input) {
    /*
      Lab Exercise 2:

      For each "normal" event passed in, write code to do the following in order:

      1) enrich the event by adding certification status, wage plan, hrs driven, miles driven and weather data
      2) call the evaluateJPMMLModel and get the prediction for the enriched event.
      3) emit the prediction + enriched event. See the declareOutputFields() method
      implementation to understand the fields to be emitted.
      4) log each outgoing event with prediction info and other details

      Here's code scaffolding to help you get started
     */

    String eventType = input.getStringByField("eventType");
    boolean violationPredicted;
    if (eventType.equals("Normal")) {
      // enrich event by calling enrichEvent(Tuple input)
      // run the enriched input through the model and predict violation
      // emit the event:
      // collector.emit( /* what goes here? */  );
      // do logging:
      // LOG.info(prediction + " predicted for " + driverName + " along route " + routeName);
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
    /*
      Lab Exercise 1: Using CSVParser, parse the timesheet.csv and drivers.csv and populate the driverTable and
      timesheetTable maps
     */

  }

  private double[] enrichEvent(Tuple input) {
    int driverID = input.getIntegerByField("driverId");
    double certified = 0, wageplan = 0, hours_logged = 0, miles_logged = 0, foggy = 0, rainy = 0, windy = 0;
    int weekNum = getWeek(input);

    /*
      Uncomment the next 4 lines once you've implemented the buildDriverRecords() method in Lab Exercise 1
     */

    //certified = driverTable.get(driverID).isCertified;
    //wageplan = driverTable.get(driverID).wagePlan.equals("miles") ? 1 : 0;
    //hours_logged = timesheetTable.get(driverID).get(weekNum).hoursLogged;
    //miles_logged = timesheetTable.get(driverID).get(weekNum).milesLogged;

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


