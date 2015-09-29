package com.hortonworks.streaming.impl.domain.rental;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

//import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import com.hortonworks.streaming.impl.domain.AbstractEventEmitter;
import com.hortonworks.streaming.impl.messages.EmitEvent;

public class RequestInitiator extends AbstractEventEmitter {
	private static final long serialVersionUID = 1L;
	private long nextransactionId = 1555775618;
	private static String[] airportCodes = { "ABI", "ACT", "AMA", "ATW", "AUS",
			"BLV", "BMI", "BPT", "BRO", "BWD", "CGI", "CLL", "CMI", "COU",
			"CRP", "CWA", "DEC", "DFW", "EAU", "ELP", "GGG", "GRB", "HRL",
			"ILE", "IRK", "JLN", "JVL", "LBB", "LRD", "LSE", "MAF", "MCI",
			"MDH", "MFE", "MKE", "MLI", "MSN", "MWA", "OSH", "PIA", "SAT",
			"SGF", "SJT", "SPI", "SPS", "STL", "TBN", "TYR", "UIN", "VCT",
			"YFC", "YSJ", "YSL" };
	private Random rand = new Random();

	@Override
	public EdifactRequestEvent generateEvent() {
		String nextPickupDate = getNextPickupDate();
		String nextReturnDate = getNextReturnDate(nextPickupDate);
		return new EdifactRequestEvent(nextransactionId++, getNextAirportCode(),
				nextPickupDate, getNextPickupTime(), nextReturnDate,
				getNextReturnTime(), getNextIata());
	}

	private String getNextPickupDate() {
		return getNextTimeRelativeDate(null);
	}

	private String getNextPickupTime() {
		String[] minutes = { "00", "15", "30", "45" };
		return getNextPaddedInt(24) + minutes[rand.nextInt(minutes.length - 1)];
	}

	private String getNextReturnDate(String pickupDate) {
		return getNextTimeRelativeDate(pickupDate);
	}

	private String getNextReturnTime() {
		String[] minutes = { "00", "15", "30", "45" };
		return getNextPaddedInt(24) + minutes[rand.nextInt(minutes.length - 1)];
	}

	private String getNextIata() {
		return "07560685";
	}

	private String getNextAirportCode() {
		return airportCodes[rand.nextInt(airportCodes.length - 1)];
	}

	private String getNextPaddedInt(int below) {
		return String.format("%02d", rand.nextInt(below));
	}

	private int getNextInt(int above, int below) {
		int nextInt = rand.nextInt(below);
		while (nextInt < above)
			nextInt = rand.nextInt(below);
		return nextInt;
	}

	private String getNextTimeRelativeDate(String pickupDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date today = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(today);

		Calendar futureCal = Calendar.getInstance();
		futureCal.setTime(today);
		futureCal.set(Calendar.MONTH, getNextInt(cal.get(Calendar.MONTH), 12));
		futureCal
				.set(Calendar.DAY_OF_MONTH,
						getNextInt(1, futureCal
								.getActualMaximum(Calendar.DAY_OF_MONTH)));
		if (pickupDate == null) {
			if (futureCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH))
				futureCal.set(
						Calendar.DAY_OF_MONTH,
						getNextInt(cal.get(Calendar.DAY_OF_MONTH),
								cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
		} else {
			try {
				sdf = new SimpleDateFormat("yyMMdd");
				Date parsedDate = sdf.parse(pickupDate);
				futureCal.setTime(parsedDate);
				futureCal.add(Calendar.DAY_OF_MONTH, rand.nextInt(14));
			} catch (ParseException e) {
			}
		}

		sdf = new SimpleDateFormat("yyMMdd");
		return sdf.format(futureCal.getTime());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof EmitEvent) {
			// Create the Service Source that will broadcast and receive
			// responses from the providers
			ActorRef serviceSource = this.context().actorOf(
					Props.create(AggregationServiceSource.class),
					"serviceSource");

			// Run the simulation
			while (true) {
				EdifactRequestEvent nextEvent = generateEvent();
				serviceSource.tell(nextEvent, this.getSelf());

				// Introduce random space between requests
				int sleepOffset = rand.nextInt(100);
				Thread.sleep(500 + sleepOffset);
			}
		}
	}
}
