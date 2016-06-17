package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import com.hortonworks.streaming.impl.TruckEventRuleEngine;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;

public class TruckEventRuleBolt implements IRichBolt {


  private static final long serialVersionUID = 6816706717943954742L;
  private static final Logger LOG = Logger.getLogger(TruckEventRuleBolt.class);

  private OutputCollector collector;
  private TruckEventRuleEngine ruleEngine;

  public TruckEventRuleBolt(Properties kafkaConfig) {
    this.ruleEngine = new TruckEventRuleEngine(kafkaConfig);
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context,
                      OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple input) {
    int driverId = input.getIntegerByField("driverId");
    String driverName = input.getStringByField("driverName");
    int routeId = input.getIntegerByField("routeId");
    String routeName = input.getStringByField("routeName");
    int truckId = input.getIntegerByField("truckId");
    Timestamp eventTime = (Timestamp) input.getValueByField("eventTime");
    String eventType = input.getStringByField("eventType");
    double longitude = input.getDoubleByField("longitude");
    double latitude = input.getDoubleByField("latitude");
    long correlationId = input.getLongByField("correlationId");


    LOG.info("Processing truck event[" + eventType + "]  for driverId[" + driverId + "], truck[" + truckId + "], " +
        "route[" + routeName + "], correlationId[" + correlationId + "]");
    ruleEngine.processEvent(driverId, driverName, routeId, truckId, eventTime, eventType, longitude, latitude,
        correlationId, routeName);
    collector.ack(input);
  }

  @Override
  public void cleanup() {
    ruleEngine.cleanUpResources();
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    return null;
  }
}
