package com.hortonworks.streaming.impl.utils;

import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class UtilTest {

  @Test
  public void date() {
    long value = 1404096345780l;
    //long value = new Date().getTime();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date(value);
    System.out.println("date is " + dateFormat.format(date));

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    System.out.println("hour is " + hour);

    System.out.println("************************ in UTC **********************");
    SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
    dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
    System.out.println("date in UTC is " + dateFormat2.format(date));

    Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar2.setTime(date);
    hour = calendar2.get(Calendar.HOUR_OF_DAY);
    System.out.print("hour in UTC is " + hour);

  }

  @Test
  public void getTime() {
    String value = "truckEventshdfs_bolt-9-6-1403818020216.txt";
    int startIndex = value.lastIndexOf("-");
    int endIndex = value.lastIndexOf(".");
    System.out.println(value.substring(startIndex + 1, endIndex));
  }

  @Test
  public void pathTest() {
    Path path = new Path("test");
    System.out.println(path.toString());
  }

  @Test
  public void time() {
    String time = "60";
    Long value = Long.valueOf(time);
    System.out.println(value);
  }

}
