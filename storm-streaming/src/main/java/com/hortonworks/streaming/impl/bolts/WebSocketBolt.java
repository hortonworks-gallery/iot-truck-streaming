package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import poc.hortonworks.domain.transport.TruckDriverViolationEvent;

import javax.jms.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

public class WebSocketBolt implements IRichBolt {


  private static final long serialVersionUID = -5319490672681173657L;
  private static final Logger LOG = Logger.getLogger(WebSocketBolt.class);

  private OutputCollector collector;
  private Properties config;
  private String user;
  private String password;
  private String activeMQConnectionString;
  private String topicName;


  private boolean sendAllEventsToTopic;
  private String allEventsTopicName;


  public WebSocketBolt(Properties config) {
    this.config = config;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context,
                      OutputCollector collector) {
    this.collector = collector;
    this.user = config.getProperty("notification.topic.user");
    this.password = config.getProperty("notification.topic.password");
    this.activeMQConnectionString = config.getProperty("notification.topic.connection.url");
    this.topicName = config.getProperty("notification.topic.events.name");

    this.sendAllEventsToTopic = Boolean.valueOf(config.getProperty("notification.all.events.notification.topic"))
        .booleanValue();
    this.allEventsTopicName = config.getProperty("notification.all.events.notification.topic.name");
  }

  @Override
  public void execute(Tuple input) {
    LOG.info("About to process tuple[" + input + "]");


    int driverId = input.getIntegerByField("driverId");
    int truckId = input.getIntegerByField("truckId");
    Timestamp eventTime = (Timestamp) input.getValueByField("eventTime");
    long eventTimeLong = eventTime.getTime();
    SimpleDateFormat sdf = new SimpleDateFormat();
    String timeStampString = sdf.format(eventTimeLong);
    String eventType = input.getStringByField("eventType");
    double longitude = input.getDoubleByField("longitude");
    double latitude = input.getDoubleByField("latitude");
    long numberOfInfractions = input.getLongByField("incidentTotalCount");
    String driverName = input.getStringByField("driverName");
    int routeId = input.getIntegerByField("routeId");
    String routeName = input.getStringByField("routeName");

    String event = constructEvent(driverId, truckId, eventTimeLong,
        timeStampString, eventType, longitude, latitude,
        numberOfInfractions, driverName, routeId, routeName);

    if (!eventType.equals("Normal")) {
      sendEventToTopic(event, this.topicName);
    }
    if (sendAllEventsToTopic) {
      sendEventToTopic(event, this.allEventsTopicName);
    }

    collector.ack(input);


  }

  public String constructEvent(int driverId, int truckId, long eventTimeLong,
                               String timeStampString, String eventType, double longitude,
                               double latitude, long numberOfInfractions, String driverName, int routeId, String
                                   routeName) {

    String truckDriverEventKey = driverId + "|" + truckId;
    TruckDriverViolationEvent driverInfraction = new TruckDriverViolationEvent(truckDriverEventKey, driverId,
        truckId, eventTimeLong, timeStampString, longitude, latitude, eventType, numberOfInfractions, driverName,
        routeId, routeName);
    ObjectMapper mapper = new ObjectMapper();
    String event = null;
    try {
      event = mapper.writeValueAsString(driverInfraction);
    } catch (Exception e) {
      LOG.error("Error converting TruckDriverViolationEvent to JSON");
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
      LOG.error("Error sending TruckDriverViolationEvent to topic", e);
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


  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    return null;
  }

  private MessageProducer getTopicProducer(Session session, String topic) {
    try {
      Topic topicDestination = session.createTopic(topic);
      MessageProducer topicProducer = session.createProducer(topicDestination);
      topicProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      return topicProducer;
    } catch (JMSException e) {
      LOG.error("Error creating producer for topic", e);
      throw new RuntimeException("Error creating producer for topic");
    }
  }


  @Override
  public void cleanup() {

  }

}
