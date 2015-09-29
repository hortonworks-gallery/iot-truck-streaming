package com.hortonworks.streaming.impl.utils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Properties;

public class EventMailer implements Serializable {

  private static final long serialVersionUID = -1336390721063108193L;
  private Properties props;

  public EventMailer(Properties config) {
    this.props = config;
  }

  public void sendEmail(String sender, String recipient,
                        String subject, String body) {

    Session session = Session.getInstance(props);

    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(sender));
      message.setRecipients(Message.RecipientType.TO,
          InternetAddress.parse(recipient));

      message.setSubject(subject);
      message.setText(body);

      Transport.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
}
