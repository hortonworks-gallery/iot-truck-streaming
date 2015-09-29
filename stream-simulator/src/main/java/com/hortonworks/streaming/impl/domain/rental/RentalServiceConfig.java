package com.hortonworks.streaming.impl.domain.rental;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class RentalServiceConfig {
	private static LinkedHashMap<String, HashMap<String, String>> services = new LinkedHashMap<String, HashMap<String, String>>();
	private static Iterator<String> nextServiceIterator = null;

	static {
		HashMap<String, String> ehiOpts = new HashMap<String, String>();
		ehiOpts.put("companyName", "ehi");
		ehiOpts.put("baseRate", "22.65");
		ehiOpts.put("carClassMultiplier", "0.3");
		ehiOpts.put("rateChangeEveryEvents", "10");
		services.put("ehi", ehiOpts);

		HashMap<String, String> hertzOpts = new HashMap<String, String>();
		hertzOpts.put("companyName", "hertz");
		hertzOpts.put("baseRate", "25.29");
		hertzOpts.put("carClassMultiplier", "0.4");
		hertzOpts.put("rateChangeEveryEvents", "100");
		services.put("hertz", hertzOpts);

		HashMap<String, String> alamoOpts = new HashMap<String, String>();
		alamoOpts.put("companyName", "alamo");
		alamoOpts.put("baseRate", "25.29");
		alamoOpts.put("carClassMultiplier", "0.4");
		alamoOpts.put("rateChangeEveryEvents", "50");
		services.put("alamo", alamoOpts);
	}

	public static HashMap<String, HashMap<String, String>> getServices() {
		return services;
	}

	public void setServices(
			LinkedHashMap<String, HashMap<String, String>> services) {
		this.services = services;
	}

	public static synchronized String getNextServiceId() {
		if (nextServiceIterator == null)
			nextServiceIterator = services.keySet().iterator();
		String nextServiceId = null;
		try {
			nextServiceId = nextServiceIterator.next();
		} catch (Exception e) {
			nextServiceIterator = services.keySet().iterator();
			nextServiceId = nextServiceIterator.next();
		}
		return nextServiceId;
	}
}
