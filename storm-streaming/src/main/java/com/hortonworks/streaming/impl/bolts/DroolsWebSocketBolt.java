package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import javax.jms.*;
import java.util.Map;
import java.util.Properties;

public class DroolsWebSocketBolt implements IRichBolt {


  private static final Logger LOG = Logger.getLogger(DroolsWebSocketBolt.class);

  private OutputCollector collector;
  private Properties config;
  private String user;
  private String password;
  private String activeMQConnectionString;
  private String topicName;


  public DroolsWebSocketBolt(Properties config) {
    this.config = config;
  }


  public void prepare(Map stormConf, TopologyContext context,
                      OutputCollector collector) {
    this.collector = collector;
    this.user = config.getProperty("notification.topic.user");
    this.password = config.getProperty("notification.topic.password");
    this.activeMQConnectionString = config.getProperty("notification.topic.connection.url");
    this.topicName = config.getProperty("drools.topic.events.name");

  }


  public void execute(Tuple input) {
    LOG.info("About to process tuple[" + input + "]");

    String eventType = input.getStringByField("eventType");
    String driverName = input.getStringByField("driverName");
    String routeName = input.getStringByField("routeName");
    int driverId = input.getIntegerByField("driverId");
    int truckId = input.getIntegerByField("truckId");
    String eventTime = input.getStringByField("timeStamp");
    double longitude = input.getDoubleByField("longitude");
    double latitude = input.getDoubleByField("latitude");
    boolean raiseAlert = input.getBooleanByField("raiseAlert");

    String event = constructEvent(eventType, driverName,
        routeName, driverId, truckId, eventTime,
        longitude, latitude, raiseAlert);

    sendEventToTopic(event, this.topicName);

    collector.ack(input);


  }

  public String constructEvent(String eventType, String driverName,
                               String routeName, int driverId, int truckId, String timeStamp,
                               double longitude, double latitude, boolean raiseAlert) {


    TruckEvent eventobj = new TruckEvent(driverId, driverName,
        routeName, truckId, timeStamp, eventType,
        longitude, latitude, raiseAlert);

    ObjectMapper mapper = new ObjectMapper();
    String event = null;
    try {
      event = mapper.writeValueAsString(eventobj);
    } catch (Exception e) {
      LOG.error("Error converting Prediction Event to JSON");
    }
    return event;
  }

  private void sendEventToTopic(String event, String topic) {
    Session session = null;
    Connection connection = null;
    try {
      ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password,
          activeMQConnectionString);
      connection = connectionFactory.createConnection();

      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);


      TextMessage message = session.createTextMessage(event);
      getTopicProducer(session, topic).send(message);
    } catch (JMSException e) {
      LOG.error("Error sending Prediction event to topic", e);
      return;
    } finally {
      if (session != null) {
        try {
          session.close();
        } catch (JMSException e) {
          LOG.error("Error cleaning up ActiveMQ resources", e);
        }
      }
      if (connection != null) {
        try {
          connection.close();
        } catch (JMSException e) {
          LOG.error("Error closing ActiveMQ connectino", e);
        }
      }

    }
  }


  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // TODO Auto-generated method stub

  }


  public Map<String, Object> getComponentConfiguration() {
    // TODO Auto-generated method stub
    return null;
  }

  private MessageProducer getTopicProducer(Session session, String topic) {
    try {
      javax.jms.Topic topicDestination = session.createTopic(topic);
      MessageProducer topicProducer = session.createProducer(topicDestination);
      topicProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      return topicProducer;
    } catch (JMSException e) {
      LOG.error("Error creating producer for topic", e);
      throw new RuntimeException("Error creating producer for topic");
    }
  }


  public void cleanup() {
    // TODO Auto-generated method stub

  }

}


