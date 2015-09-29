package poc.hortonworks.storm.demoreset.web;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import poc.hortonworks.storm.demoreset.service.DemoResetService;

@Controller
public class DemoResetController {

	private static Logger LOG = Logger.getLogger(DemoResetController.class);
	private DemoResetService demoResetService;
	
	@Autowired
	public DemoResetController(DemoResetService service) {
		this.demoResetService = service;
	}
	
	@MessageMapping("/resetDemo")
	public void resetDemo(DemoResetParam param) {
		LOG.info("truncateHBaseTables value is" + param.isTruncateHbaseTables());
		demoResetService.resetDemo(param);
	}
}
