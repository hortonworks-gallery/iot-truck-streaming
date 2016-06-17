package com.hortonworks.streaming.impl.topologies;


import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import com.hortonworks.streaming.impl.bolts.*;
import com.hortonworks.streaming.impl.bolts.hdfs.FileTimeRotationPolicy;
import com.hortonworks.streaming.impl.bolts.hive.HiveTablePartitionAction;
import com.hortonworks.streaming.impl.kafka.TruckScheme2;
import org.apache.log4j.Logger;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.ZkHosts;

public class TruckEventProcessorKafkaTopology extends BaseTruckEventTopology {

  private static final Logger LOG = Logger.getLogger(TruckEventProcessorKafkaTopology.class);
  private String predictionBoltName = "prediction_bolt";

  public TruckEventProcessorKafkaTopology(String configFileLocation) throws Exception {
    super(configFileLocation);
  }

  public static void main(String[] args) throws Exception {
    String configFileLocation = args[0];
    TruckEventProcessorKafkaTopology truckTopology = new TruckEventProcessorKafkaTopology(configFileLocation);
    truckTopology.buildAndSubmit();
  }

  public void buildAndSubmit() throws Exception {
    TopologyBuilder builder = new TopologyBuilder();

		/* Set up Kafka Spout to ingest from */
    configureKafkaSpout(builder);

		/* Set up HDFSBOlt to send every truck event to HDFS */
    configureHDFSBolt(builder);

		/* Setup Monitoring Bolt to track number of alerts per truck driver */
    configureMonitoringBolt(builder);

		/* Setup HBse Bolt for to persist violations and all events (if configured to do so)*/
    configureHBaseBolt(builder);

		/* Setup WebSocket Bolt for alerts and notifications */
    configureWebSocketBolt(builder);

		/* Setup Prediction Bolt for calling ML model */
    configurePredictionBolt(builder);

		/* Setup Prediction Web Socket Bolt for prediction alerts */
    configurePredictionSocketBolt(builder);

    configureDroolsBolt(builder);

    configureDroolsWebSocketBolt(builder);


		/* This conf is for Storm and it needs be configured with things like the following:
		 * 	Zookeeper server, nimbus server, ports, etc... All of this configuration will be picked up
		 * in the ~/.storm/storm.yaml file that will be located on each storm node.
		 */
    Config conf = new Config();
    conf.setDebug(true);
		/* Set the number of workers that will be spun up for this topology.
		 * Each worker represents a JVM where executor thread will be spawned from */
    Integer topologyWorkers = Integer.valueOf(topologyConfig.getProperty("storm.trucker.topology.workers"));
    conf.put(Config.TOPOLOGY_WORKERS, topologyWorkers);

    //Read the nimbus host in from the config file as well
    String nimbusHost = topologyConfig.getProperty("nimbus.host");
    conf.put(Config.NIMBUS_HOST, nimbusHost);

    try {
      StormSubmitter.submitTopology("truck-event-processor", conf, builder.createTopology());
    } catch (Exception e) {
      LOG.error("Error submiting Topology", e);
    }

  }

  public void configureWebSocketBolt(TopologyBuilder builder) {
    boolean configureWebSocketBolt = Boolean.valueOf(topologyConfig.getProperty("notification.topic")).booleanValue();
    if (configureWebSocketBolt) {
      WebSocketBolt webSocketBolt = new WebSocketBolt(topologyConfig);
      builder.setBolt("web_sockets_bolt", webSocketBolt, 4).shuffleGrouping("hbase_bolt");
    }
  }

  public void configureHBaseBolt(TopologyBuilder builder) {
    TruckHBaseBolt hbaseBolt = new TruckHBaseBolt(topologyConfig);
    builder.setBolt("hbase_bolt", hbaseBolt, 2).shuffleGrouping("kafkaSpout");
  }

  /**
   * Send truckEvents from same driver to the same bolt instances to maintain accuracy of eventCount per truck/driver
   *
   * @param builder
   */
  public void configureMonitoringBolt(TopologyBuilder builder) {
    int boltCount = Integer.valueOf(topologyConfig.getProperty("bolt.thread.count"));
    builder.setBolt("monitoring_bolt",
        new TruckEventRuleBolt(topologyConfig), boltCount)
        .fieldsGrouping("kafkaSpout", new Fields("driverId"));
  }

  public void configureHDFSBolt(TopologyBuilder builder) {
    // Use pipe as record boundary

    String rootPath = topologyConfig.getProperty("hdfs.path");
    String prefix = topologyConfig.getProperty("hdfs.file.prefix");
    String fsUrl = topologyConfig.getProperty("hdfs.url");
    String sourceMetastoreUrl = topologyConfig.getProperty("hive.metastore.url");
    String hiveStagingTableName = topologyConfig.getProperty("hive.staging.table.name");
    String databaseName = topologyConfig.getProperty("hive.database.name");
    Float rotationTimeInMinutes = Float.valueOf(topologyConfig.getProperty("hdfs.file.rotation.time.minutes"));

    RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter(",");

    //Synchronize data buffer with the filesystem every 1000 tuples
    SyncPolicy syncPolicy = new CountSyncPolicy(1000);

    // Rotate data files when they reach five MB
    //FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, Units.MB);

    //Rotate every X minutes
    FileTimeRotationPolicy rotationPolicy = new FileTimeRotationPolicy(rotationTimeInMinutes, FileTimeRotationPolicy
        .Units.MINUTES);

    //Hive Partition Action
    HiveTablePartitionAction hivePartitionAction = new HiveTablePartitionAction(sourceMetastoreUrl,
        hiveStagingTableName, databaseName, fsUrl);

    //MoveFileAction moveFileAction = new MoveFileAction().toDestination(rootPath + "/working");


    FileNameFormat fileNameFormat = new DefaultFileNameFormat()
        .withPath(rootPath + "/staging")
        .withPrefix(prefix);

    // Instantiate the HdfsBolt
    HdfsBolt hdfsBolt = new HdfsBolt()
        .withFsUrl(fsUrl)
        .withFileNameFormat(fileNameFormat)
        .withRecordFormat(format)
        .withRotationPolicy(rotationPolicy)
        .withSyncPolicy(syncPolicy)
        .addRotationAction(hivePartitionAction);

    int hdfsBoltCount = Integer.valueOf(topologyConfig.getProperty("hdfsbolt.thread.count"));
    builder.setBolt("hdfs_bolt", hdfsBolt, hdfsBoltCount).shuffleGrouping("kafkaSpout");
  }

  public int configurePredictionBolt(TopologyBuilder builder) {
    // Check config and choose the appropriate prediction bolt
    if (topologyConfig.getProperty("prediction.engine").equalsIgnoreCase("jpmml")) {
      predictionBoltName = "prediction_bolt_jpmml";
      builder.setBolt(predictionBoltName, new PredictionBoltJPMML(topologyConfig), 1).shuffleGrouping("kafkaSpout");
    } else {
      predictionBoltName = "prediction_bolt_mllib";
      builder.setBolt(predictionBoltName, new PredictionBolt(topologyConfig), 1).shuffleGrouping("kafkaSpout");
    }

    int boltCount = Integer.valueOf(topologyConfig.getProperty("bolt.thread.count"));

    return boltCount;
  }

  public void configurePredictionSocketBolt(TopologyBuilder builder) {
    boolean configurePredictionSocketBolt = Boolean.valueOf(topologyConfig.getProperty("prediction.topic"))
        .booleanValue();
    if (configurePredictionSocketBolt) {
      PredictionWebSocketBolt webSocketBolt = new PredictionWebSocketBolt(topologyConfig);
      builder.setBolt("prediction_sockets_bolt", webSocketBolt, 5).shuffleGrouping(predictionBoltName);
    }
  }

  public int configureDroolsBolt(TopologyBuilder builder) {
    builder.setBolt("drool_bolt", new DroolsBolt(), 1).fieldsGrouping("kafkaSpout", new Fields("driverId"));

    int boltCount = Integer.valueOf(topologyConfig.getProperty("bolt.thread.count"));

    return boltCount;
  }

  public int configureDroolsWebSocketBolt(TopologyBuilder builder) {
    builder.setBolt("droolws_bolt", new DroolsWebSocketBolt(topologyConfig), 1).shuffleGrouping("drool_bolt");
    int boltCount = Integer.valueOf(topologyConfig.getProperty("bolt.thread.count"));

    return boltCount;
  }

  public int configureKafkaSpout(TopologyBuilder builder) {
    KafkaSpout kafkaSpout = constructKafkaSpout();

    int spoutCount = Integer.valueOf(topologyConfig.getProperty("spout.thread.count"));
    int boltCount = Integer.valueOf(topologyConfig.getProperty("bolt.thread.count"));

    builder.setSpout("kafkaSpout", kafkaSpout, spoutCount);
    return boltCount;
  }

  private KafkaSpout constructKafkaSpout() {
    KafkaSpout kafkaSpout = new KafkaSpout(constructKafkaSpoutConf());
    return kafkaSpout;
  }

  private SpoutConfig constructKafkaSpoutConf() {
    BrokerHosts hosts = new ZkHosts(topologyConfig.getProperty("kafka.zookeeper.host.port"));
    String topic = topologyConfig.getProperty("kafka.topic");
    String zkRoot = topologyConfig.getProperty("kafka.zkRoot");
    String consumerGroupId = topologyConfig.getProperty("kafka.consumer.group.id");

    SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, zkRoot, consumerGroupId);

		/* Custom TruckScheme that will take Kafka message of single truckEvent
		 * and emit a 2-tuple consisting of truckId and truckEvent. This driverId
		 * is required to do a fieldsSorting so that all driver events are sent to the set of bolts */
    spoutConfig.scheme = new SchemeAsMultiScheme(new TruckScheme2());

    return spoutConfig;
  }

}





