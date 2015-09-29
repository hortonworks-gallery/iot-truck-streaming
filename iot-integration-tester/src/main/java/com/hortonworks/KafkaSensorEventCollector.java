package com.hortonworks;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;


import akka.actor.UntypedActor;
import com.hortonworks.streaming.impl.collectors.KafkaEventCollector;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import com.hortonworks.streaming.impl.domain.transport.MobileEyeEvent;
import org.apache.log4j.Logger;

public class KafkaSensorEventCollector extends UntypedActor {

  private static final String TOPIC = "truck_events";
  private Producer<String, String> kafkaProducer;
  private Properties props = new Properties();
  private Properties kafkaProperties = IotAppTesterMain.kafkaLocalBroker.getKafkaProperties();
  private Logger logger = Logger.getLogger(this.getClass());

  public KafkaSensorEventCollector() {
    kafkaProperties.put("metadata.broker.list", "localhost:20111");
    kafkaProperties.put("serializer.class", "kafka.serializer.StringEncoder");
    //kafkaProperties.put("request.required.acks", "1");
    try {
      ProducerConfig producerConfig = new ProducerConfig(kafkaProperties);
      kafkaProducer = new Producer<>(producerConfig);
    } catch (Exception e) {
      logger.error("Error creating producer", e);
    }
  }

  @Override
  public void onReceive(Object event) throws Exception {
    MobileEyeEvent mee = (MobileEyeEvent) event;
    String eventToPass = mee.toString();
    String driverId = String.valueOf(mee.getTruck().getDriver().getDriverId());

    logger.debug("Creating event[" + eventToPass + "] for driver[" + driverId + "] in truck [" + mee.getTruck() + "]");

    try {
      KeyedMessage<String, String> msg = new KeyedMessage<>(TOPIC, driverId, eventToPass);
      kafkaProducer.send(msg);
    } catch (Exception e) {
      logger.error("Error sending event[" + eventToPass + "] to Kafka queue (" +
          kafkaProperties.get("metadata.broker.list") + ")", e);
    }
  }
}
