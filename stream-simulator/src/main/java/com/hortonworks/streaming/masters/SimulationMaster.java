package com.hortonworks.streaming.masters;

import org.apache.log4j.Logger;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinRouter;

import com.hortonworks.streaming.impl.messages.EmitEvent;
import com.hortonworks.streaming.impl.messages.StartSimulation;
import com.hortonworks.streaming.impl.messages.StopSimulation;
import com.hortonworks.streaming.results.SimulationResultsSummary;

@SuppressWarnings("rawtypes")
public class SimulationMaster extends UntypedActor {
	private int numberOfEventEmitters = 1;
	private int numberOfEvents = 1;
	private Class eventEmitterClass;
	private ActorRef eventEmitterRouter;
	private ActorRef listener;
	private int eventCount = 0;
	private Logger logger = Logger.getLogger(SimulationMaster.class);

	@SuppressWarnings("unchecked")
	public SimulationMaster(int numberOfEventEmitters, Class eventEmitterClass,
			ActorRef listener, int numberOfEvents, long demoId, int messageDelay) {
		logger.info("Starting simulation with " + numberOfEventEmitters
				+ " of " + eventEmitterClass + " Event Emitters -- "
				+ eventEmitterClass.toString());
		this.listener = listener;
		this.numberOfEventEmitters = numberOfEventEmitters;
		this.eventEmitterClass = eventEmitterClass;
		eventEmitterRouter = this.getContext().actorOf(
				Props.create(eventEmitterClass, numberOfEvents, demoId, messageDelay).withRouter(
						new RoundRobinRouter(numberOfEventEmitters)),
				"eventEmitterRouter");
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof StartSimulation) {
			logger.info("Starting Simulation");
			while (eventCount < numberOfEventEmitters) {
				eventEmitterRouter.tell(new EmitEvent(), getSelf());
				eventCount++;
			}
		} else if (message instanceof StopSimulation) {
			listener.tell(new SimulationResultsSummary(eventCount), getSelf());
//			this.getContext().system().shutdown();
//			System.exit(0);
		} else {
			logger.debug("Received message I'm not sure what to do with: "
					+ message);
		}
	}
}
