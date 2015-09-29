package com.hortonworks;

import backtype.storm.Config;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.github.sakserv.minicluster.config.ConfigVars;
import com.github.sakserv.minicluster.impl.KafkaLocalBroker;
import com.github.sakserv.minicluster.impl.StormLocalCluster;
import com.github.sakserv.minicluster.impl.ZookeeperLocalCluster;
import com.hortonworks.labsolution.SparkPredictionBolt;
import com.hortonworks.streaming.impl.bolts.TruckEventRuleBolt;
import com.hortonworks.streaming.impl.kafka.TruckScheme2;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Launcher;

import java.io.IOException;
import java.util.Properties;


public class IotAppTesterMain {

  private static final Logger LOG = LoggerFactory.getLogger(IotAppTesterMain.class);
  private static PropertyParser propertyParser;
  private static final boolean DO_CLEAN_UP = true;
  protected static KafkaLocalBroker kafkaLocalBroker;

  static {
    try {
      propertyParser = new PropertyParser("default.properties");
      propertyParser.parsePropsFile();
    } catch (IOException e) {
      LOG.error("Unable to load property file: " + Launcher.class.getResource("/default.properties").getPath());
    }
  }

  public static void main(String args[]) {

    final ZookeeperLocalCluster zookeeperLocalCluster = new ZookeeperLocalCluster.Builder()
        .setPort(Integer.parseInt(propertyParser.getProperty(ConfigVars.ZOOKEEPER_PORT_KEY)))
        .setTempDir(propertyParser.getProperty(ConfigVars.ZOOKEEPER_TEMP_DIR_KEY))
        .setZookeeperConnectionString(propertyParser.getProperty(ConfigVars.ZOOKEEPER_CONNECTION_STRING_KEY))
        .build();

    kafkaLocalBroker = new KafkaLocalBroker.Builder()
        .setKafkaHostname(propertyParser.getProperty(ConfigVars.KAFKA_HOSTNAME_KEY))
        .setKafkaPort(Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_PORT_KEY)))
        .setKafkaBrokerId(Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_TEST_BROKER_ID_KEY)))
        .setKafkaProperties(new Properties())
        .setKafkaTempDir(propertyParser.getProperty(ConfigVars.KAFKA_TEST_TEMP_DIR_KEY))
        .setZookeeperConnectionString(propertyParser.getProperty(ConfigVars.ZOOKEEPER_CONNECTION_STRING_KEY))
        .build();

    final StormLocalCluster stormLocalCluster = new StormLocalCluster.Builder()
        .setZookeeperHost(propertyParser.getProperty(ConfigVars.ZOOKEEPER_HOST_KEY))
        .setZookeeperPort(Long.parseLong(propertyParser.getProperty(ConfigVars.ZOOKEEPER_PORT_KEY)))
        .setEnableDebug(Boolean.parseBoolean(propertyParser.getProperty(ConfigVars.STORM_ENABLE_DEBUG_KEY)))
        .setNumWorkers(Integer.parseInt(propertyParser.getProperty(ConfigVars.STORM_NUM_WORKERS_KEY)))
        .setEnableDebug(true)
        .setStormConfig(new Config())
        .build();


    try {
      zookeeperLocalCluster.start();
      kafkaLocalBroker.start();
      stormLocalCluster.start();
    } catch (Exception e) {
      LOG.error("Couldn't start the services: " + e.getMessage());
      e.printStackTrace();
    }


    SensorEventsParam sensorEventsParam = new SensorEventsParam();
    sensorEventsParam.setEventEmitterClassName("com.hortonworks.streaming.impl.domain.transport.Truck");
    sensorEventsParam.setEventCollectorClassName("com.hortonworks.KafkaSensorEventCollector");
    sensorEventsParam.setNumberOfEvents(200);
    sensorEventsParam.setDelayBetweenEvents(1000);
    sensorEventsParam.setRouteDirectory(Launcher.class.getResource("/" + "routes/midwest").getPath());
    sensorEventsParam.setTruckSymbolSize(10000);
    SensorEventsGenerator sensorEventsGenerator = new SensorEventsGenerator();
    sensorEventsGenerator.generateTruckEventsStream(sensorEventsParam);

    BrokerHosts hosts = new ZkHosts(zookeeperLocalCluster.getZookeeperConnectionString());
    String topic = "truck_events";
    String zkRoot = "/";
    String consumerGroupId = "group1";
    SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, zkRoot, consumerGroupId);
    spoutConfig.scheme = new SchemeAsMultiScheme(new TruckScheme2());
    KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
    int spoutCount = 1;
    int boltCount = 1;

    TopologyBuilder builder = new TopologyBuilder();
    builder.setSpout("kafkaSpout", kafkaSpout, spoutCount);

    /*
    builder.setBolt("monitoring_bolt",
        new TruckEventRuleBolt(propertyParser.props), boltCount)
        .fieldsGrouping("kafkaSpout", new Fields("driverId"));
        */

    builder.setBolt("spark_prediction_bolt",
        new SparkPredictionBolt(), boltCount)
        .fieldsGrouping("kafkaSpout", new Fields("driverId"));
    // IMPORTANT: create topology before submitting to cluster
    StormTopology topology = builder.createTopology();
    stormLocalCluster.submitTopology("IOT-Tester2", stormLocalCluster.getStormConf(), topology);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          stormLocalCluster.stop(DO_CLEAN_UP);
          kafkaLocalBroker.stop(DO_CLEAN_UP);
          zookeeperLocalCluster.stop(DO_CLEAN_UP);
        } catch (Exception e) {
          LOG.error("Couldn't shutdown the services: " + e.getLocalizedMessage());
          e.printStackTrace();
        }
      }
    });

    while(true) {
      // run until ctrl-c'd or stopped from IDE
    }
  }
}
