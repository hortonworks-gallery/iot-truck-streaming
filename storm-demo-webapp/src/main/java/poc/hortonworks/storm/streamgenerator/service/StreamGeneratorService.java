package poc.hortonworks.storm.streamgenerator.service;

import java.util.Random;

import org.springframework.stereotype.Service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.hortonworks.streaming.impl.domain.transport.TruckConfiguration;
import com.hortonworks.streaming.impl.messages.StartSimulation;
import com.hortonworks.streaming.impl.messages.StopSimulation;
import com.hortonworks.streaming.listeners.SimulatorListener;
import com.hortonworks.streaming.masters.SimulationMaster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Service
public class StreamGeneratorService {
	
	
	//Default Coordinates for Saint Louis
	public static final double STL_LAT= 38.523884;
	public static final double STL_LONG= -92.159845;
	public static final int DEFAULT_ZOOME_LEVEL = 4;
	public static final int DEFAULT_TRUCK_SYMBOL_SIZE = 10000;
	
	public double centerCoordinatesLat = STL_LAT;
	public double centerCoordinatesLong = STL_LONG;
	public int zoomLevel = DEFAULT_ZOOME_LEVEL;
	public int truckSymbolSize = DEFAULT_TRUCK_SYMBOL_SIZE;
	
	public void generateTruckEventsStream(final StreamGeneratorParam params) {

		try {
			
			final Class eventEmitterClass = Class.forName(params.getEventEmitterClassName());
			final Class eventCollectorClass = Class.forName(params.getEventCollectorClassName());
			
			Config config= ConfigFactory.load();
		
			this.centerCoordinatesLat = params.getCenterCoordinatesLat();
			this.centerCoordinatesLat = params.getCenterCoordinatesLong();
			this.zoomLevel = params.getZoomLevel();	
			this.truckSymbolSize = params.getTruckSymbolSize();
			TruckConfiguration.initialize(params.getRouteDirectory());
			int emitters=TruckConfiguration.freeRoutePool.size();
			
			Thread.sleep(5000);
			System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& Number of Emitters is ....." + emitters);
			
			ActorSystem system = ActorSystem.create("EventSimulator", config, getClass().getClassLoader());
			final ActorRef listener = system.actorOf(
					Props.create(SimulatorListener.class), "listener");
			final ActorRef eventCollector = system.actorOf(
					Props.create(eventCollectorClass), "eventCollector");
			final int numberOfEmitters = emitters;
			System.out.println(eventCollector.path());
			final long demoId = new Random().nextLong();
			final ActorRef master = system.actorOf(new Props(
					new UntypedActorFactory() {
						public UntypedActor create() {
							return new SimulationMaster(
									numberOfEmitters,
									/*eventEmitterClass, listener, params.getNumberOfEvents(), demoId);*/
									eventEmitterClass, listener, params.getNumberOfEvents(), demoId, params.getDelayBetweenEvents());
						}
					}), "master");
			master.tell(new StartSimulation(), master);
		} catch (Exception e) {
			throw new RuntimeException("Error running truck stream generator", e);
		} 
	
	}
	
	public void resetMapCords() {
		this.centerCoordinatesLat = STL_LAT;
		this.centerCoordinatesLong = STL_LONG;
		this.zoomLevel = DEFAULT_ZOOME_LEVEL;
		this.truckSymbolSize = DEFAULT_TRUCK_SYMBOL_SIZE;
	}

}


//20 -1 com.hortonworks.streaming.impl.domain.transport.Truck com.hortonworks.streaming.impl.collectors.KafkaEventCollector
