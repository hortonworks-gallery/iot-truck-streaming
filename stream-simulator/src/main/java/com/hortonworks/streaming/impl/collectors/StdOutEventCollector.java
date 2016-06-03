package com.hortonworks.streaming.impl.collectors;

import com.hortonworks.streaming.impl.domain.AbstractEventCollector;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

public class StdOutEventCollector extends AbstractEventCollector {

	protected BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
	
	@Override
	public void onReceive(Object message) throws Exception {
		log.write( message.toString() + "\n" );
		log.flush();
	}


}
