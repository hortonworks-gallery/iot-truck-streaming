package poc.hortonworks.storm.streamgenerator.web;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import poc.hortonworks.storm.streamgenerator.service.StreamGeneratorParam;
import poc.hortonworks.storm.streamgenerator.service.StreamGeneratorService;

@Controller
public class StreamGeneratorController {

	private static final Logger LOG = Logger.getLogger(StreamGeneratorController.class);
	
	private StreamGeneratorService streamGeneratorService;
	
	@Autowired
	public StreamGeneratorController(StreamGeneratorService service) {
		this.streamGeneratorService = service;
	}

	@MessageMapping("/mapTruckRoutes")
	public void mapTruckRoutes(StreamGeneratorParam param) {
		LOG.info("Starting Mapping Truck Routes...");
		streamGeneratorService.generateTruckEventsStream(param);
	}
	
}
