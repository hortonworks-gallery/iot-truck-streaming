package com.hortonworks.streaming.impl.collectors;

import com.hortonworks.streaming.impl.domain.AbstractEventCollector;

public class DefaultEventCollector extends AbstractEventCollector {

	
	@Override
	public void onReceive(Object message) throws Exception {
		logger.info(message);
	}


}
