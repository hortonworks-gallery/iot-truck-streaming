Data Simulator
================

The goal of this project is to create a very lightweight framework to quickly generate data for demo purposes.  
The framework uses Akka to simplify concurrency and messaging, and is simple to extend.  

The concept is that we have domain objects, for example Trucks.  
These Trucks generate events, events in this case being influenced by their driver's risk factor.  
There are two PoJos to model this.  
One for [Truck] (Truck.java), and the other for [Driver] (Driver.java).  
These two domain objects are used to generate events, in this case the Truck class generates events, and as such 
extends the AbstractEventEmitter class.  By implementing this class, the onReceive method needs to be implemented.  
This method is called for each Truck in the simulation.  The onReceive method should generate events, 
create new Actors, send messages to Actors, etc, and send those events to an EventCollector.  
The EventCollector is a class you can implement to collect the events generated from the, in this case Trucks, and put 
them somewhere.  In this case, we're using the JmsEventCollector to push events to a ActiveMQ JMS queue.

## Extending the framework

To implement a new simulation, create a PoJo that implements the AbstractEventEmitter class.  
Implement the onReceive method as follows:

	@Override
	public void onReceive(Object message) throws Exception {
	    if (message instanceof EmitEvent) {
	        ActorRef actor = this.context().system()
	                .actorFor("akka://EventSimulator/user/eventCollector");
	        Random rand = new Random();
	        int sleepOffset = rand.nextInt(100);
	        while (true) {
	            Thread.sleep(500 + sleepOffset);
	            actor.tell(generateEvent(), this.getSender());
	        }
	    }
	}
	
In this case, I'm making sure I have a reference to the eventCollector, and then I'm waiting a natural amount of time 
before I generate an event.  
The generateEvent class, in this case, just returns a class that extends the Event class.  
The Event is arbitrary, it could be a financial transaction, or anything.  
That Event is sent to the EventCollector to be persisted to disk in the form of a log, or put on a queue, etc.

## Building the simulator

This is built using maven, so just do `mvn clean package`.

## Running the simulator

	./run.sh 6 -1 com.hortonworks.streaming.impl.domain.transport.Truck com.hortonworks.streaming.impl.collectors.JmsEventCollector

The run.sh script takes in the number of domain objects to spin up, the number of events to process before exiting 
(-1 is unlimited), and the fully qualified class name of the domain object that extends AbstractEventEmitter, 
and the EventCollector class.