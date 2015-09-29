package com.hortonworks.streaming.impl.utils;

import java.io.Serializable;
import java.util.Properties;

public class EventMailerTest implements Serializable {


  private static final long serialVersionUID = -6568782918805429303L;

  //@Test
  public void sendEmailTest() {
    Properties config = new Properties();
    config.put("mail.smtp.host", "hadoopsummit-stormapp.secloud.hortonworks.com");
    //config.put("mail.smtp.host", "192.168.17.32");
    config.put("mail.smtp.port", 25);
    EventMailer mailer = new EventMailer(config);
    mailer.sendEmail("gvetticaden@hortonworks.com", "gvetticaden@hortonworks.com", "Driving Violation", "test");
  }

}