package com.hortonworks.streaming.impl.bolts;

public class TruckEvent {

  int driverId;
  String driverName;
  String routeName;
  int truckId;
  String eventTime;
  String eventType;
  double longitude;
  double latitude;
  boolean raiseAlert;


  public TruckEvent(int driverId, String driverName,
                    String routeName, int truckId, String eventTime,
                    String eventType, double longitude, double latitude,
                    boolean raiseAlert
  ) {

    this.driverId = driverId;
    this.driverName = driverName;
    this.routeName = routeName;
    this.truckId = truckId;
    this.eventTime = eventTime;
    this.eventType = eventType;
    this.longitude = longitude;
    this.latitude = latitude;
    this.raiseAlert = raiseAlert;
  }

  public int getDriverId() {
    return driverId;
  }

  public void setDriverId(int driverId) {
    this.driverId = driverId;
  }

  public String getDriverName() {
    return driverName;
  }

  public void setDriverName(String driverName) {
    this.driverName = driverName;
  }

  public String getRouteName() {
    return routeName;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  public int getTruckId() {
    return truckId;
  }

  public void setTruckId(int truckId) {
    this.truckId = truckId;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public boolean isRaiseAlert() {
    return raiseAlert;
  }

  public void setRaiseAlert(boolean raiseAlert) {
    this.raiseAlert = raiseAlert;
  }


}

