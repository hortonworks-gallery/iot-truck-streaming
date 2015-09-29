/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package poc.hortonworks.storm.truck.web;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import poc.hortonworks.domain.transport.TruckDriverViolationEvent;
import poc.hortonworks.storm.streamgenerator.service.StreamGeneratorService;
import poc.hortonworks.storm.truck.DriverEventsResponse;
import poc.hortonworks.storm.truck.service.DriverEventsService;


@Controller
public class TruckDriverEventsController {

	private static final Log logger = LogFactory.getLog(TruckDriverEventsController.class);

	private final DriverEventsService driverEventsService;

	private StreamGeneratorService streamingService;
	
	


	@Autowired
	public TruckDriverEventsController(DriverEventsService driverEventsService, StreamGeneratorService streamingService) {
		this.driverEventsService = driverEventsService;
		this.streamingService = streamingService;
	}

	@SubscribeMapping("/driverEvents")
	public DriverEventsResponse getDriverEvents() throws Exception {
		DriverEventsResponse response = new DriverEventsResponse();
		response.setViolationEvents(driverEventsService.getLatestEventsForAllDrivers());
		response.setStartLat(streamingService.centerCoordinatesLat);
		response.setStartLong(streamingService.centerCoordinatesLong);
		//response.setZoomLevel(streamingService.zoomLevel);
		response.setZoomLevel(4);
		response.setTruckSymbolSize(streamingService.truckSymbolSize);
		
		logger.info("Start Lat is " + response.getStartLat());
		logger.info("Start Long is " + response.getStartLong());
		logger.info("Zoom level is : " + response.getZoomLevel());
		logger.info("Truck Symbol Size is : " + response.getTruckSymbolSize());
		return response;
	}


}
