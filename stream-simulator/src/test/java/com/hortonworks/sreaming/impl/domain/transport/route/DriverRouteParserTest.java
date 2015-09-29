package com.hortonworks.sreaming.impl.domain.transport.route;

import static org.junit.Assert.*;

import java.util.List;

import com.hortonworks.streaming.impl.domain.gps.Location;
import com.hortonworks.streaming.impl.domain.transport.route.Route;
import com.hortonworks.streaming.impl.domain.transport.route.TruckRoutesParser;
import com.hortonworks.streaming.impl.domain.transport.route.jaxb.Kml;

import org.junit.Test;

public class DriverRouteParserTest {
	
	@Test
	public void parseRoute() {
		TruckRoutesParser parser = new TruckRoutesParser();
		String file = "/Users/gvetticaden/Dropbox/Hortonworks/Development/Git/stream-simulator/src/test/resources/TestRoute2.xml";
		Route route =  parser.parseRoute(file);
		assertNotNull(route);
		List<Location> locations = route.getLocations();
		assertNotNull(locations);
		assertEquals(16,  locations.size());
		for(Location location : locations) {
			System.out.println("Lat:"  + location.getLatitude());
			System.out.println("Long:" + location.getLongitude());
		}
		
	}
	
	@Test
	public void parseRoutes() {
		TruckRoutesParser parser = new TruckRoutesParser();
		List<Route> routes = parser.parseAllRoutes("/Users/gvetticaden/Dropbox/Hortonworks/Development/Git/stream-simulator/src/test/resources");
		assertNotNull(routes);
		assertEquals(2, routes.size());
		
		for (Route route:routes) {
			List<Location> locations = route.getLocations();
			assertNotNull(locations);
			for(Location location : locations) {
				System.out.println("Lat:"  + location.getLatitude());
				System.out.println("Long:" + location.getLongitude());
			}
		}
	}

}
