package com.hortonworks.streaming.impl.domain.transport.route;

import java.util.List;

import com.hortonworks.streaming.impl.domain.gps.Location;

public interface Route {
	List<Location> getLocations();
	Location getNextLocation();
	Location getStartingPoint();
	boolean routeEnded();
	int getRouteId();
	String getRouteName();
}