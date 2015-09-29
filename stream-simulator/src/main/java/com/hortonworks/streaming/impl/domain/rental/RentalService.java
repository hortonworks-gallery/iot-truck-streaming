package com.hortonworks.streaming.impl.domain.rental;

import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;

import akka.actor.ActorSelection;

import com.hortonworks.streaming.impl.domain.AbstractEventEmitter;
import com.hortonworks.streaming.impl.domain.Event;

public class RentalService extends AbstractEventEmitter {
	private static final long serialVersionUID = -7707647735283442703L;
	private String rentalServiceId = new String();
	private HashMap<String, String> rentalServiceConfig = null;
	private Random rand = new Random();
	private int numberOfEvents = 0;
	private Logger logger = Logger.getLogger(RentalService.class);

	public RentalService(String rentalServiceId,
			HashMap<String, String> rentalServiceConfig) {
		this.rentalServiceId = rentalServiceId;
		this.rentalServiceConfig = rentalServiceConfig;
	}

	@Override
	public Event generateEvent() {
		return null;
	}

	private EdifactResponseEvent generateResponseFromRequest(
			EdifactRequestEvent nextEvent) {
		numberOfEvents++;
		boolean changeRate = false;
		if (numberOfEvents
				% Integer.parseInt(rentalServiceConfig
						.get("rateChangeEveryEvents")) == 0) {
			changeRate = true;
			logger.debug("Changing Rates for " + rentalServiceId);
		}
		EdifactResponseEvent responseEvent = new EdifactResponseEvent(
				nextEvent, rentalServiceConfig, changeRate);
		return responseEvent;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		ActorSelection actor = this.context().system()
				.actorSelection("akka://EventSimulator/user/eventCollector");
		if (message instanceof EdifactRequestEvent) {
			int sleepOffset = rand.nextInt(100);
			Thread.sleep(100 + sleepOffset);
			actor.tell(
					generateResponseFromRequest((EdifactRequestEvent) message),
					this.getSelf());
		}
	}

	public String getRentalServiceId() {
		return rentalServiceId;
	}

	public void setRentalServiceId(String rentalServiceId) {
		this.rentalServiceId = rentalServiceId;
	}

	public HashMap<String, String> getRentalServiceConfig() {
		return rentalServiceConfig;
	}

	public void setRentalServiceConfig(
			HashMap<String, String> rentalServiceConfig) {
		this.rentalServiceConfig = rentalServiceConfig;
	}
}
