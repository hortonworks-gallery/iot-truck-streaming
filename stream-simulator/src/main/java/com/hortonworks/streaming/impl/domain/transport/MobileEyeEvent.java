package com.hortonworks.streaming.impl.domain.transport;

import com.hortonworks.streaming.impl.domain.Event;
import com.hortonworks.streaming.impl.domain.gps.Location;

public class MobileEyeEvent extends Event {
	private MobileEyeEventTypeEnum eventType;
	private Truck truck;
	private Location location;
	private long correlationId;



	public MobileEyeEvent(long correlationId, Location location, MobileEyeEventTypeEnum eventType,
			Truck truck) {
		this.location = location;
		this.eventType = eventType;
		this.truck = truck;
		this.correlationId = correlationId;
	}

	public MobileEyeEventTypeEnum getEventType() {
		return eventType;
	}

	public void setEventType(MobileEyeEventTypeEnum eventType) {
		this.eventType = eventType;
	}

	public Location getLocation() {
		return location;
	}
	
	public Truck getTruck() {
		return this.truck;
	}

	@Override
	public String toString() {
		return truck.toString() + eventType.toString() + "|"
				+ location.getLatitude() + "|" + location.getLongitude() + "|" + correlationId;
	}
}
