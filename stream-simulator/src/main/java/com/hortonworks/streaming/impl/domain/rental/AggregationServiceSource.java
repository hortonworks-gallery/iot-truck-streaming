package com.hortonworks.streaming.impl.domain.rental;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.japi.Creator;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import com.hortonworks.streaming.impl.domain.AbstractEventEmitter;

public class AggregationServiceSource extends AbstractEventEmitter {
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
	private Router router = null;

	@Override
	public EdifactRequestEvent generateEvent() {
		String nextPickupDate = getNextPickupDate();
		String nextReturnDate = getNextReturnDate(nextPickupDate);
		return new EdifactRequestEvent(nextransactionId, getNextAirportCode(),
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
		if (router == null) {
			List<Routee> routees = new ArrayList<Routee>();
			HashMap<String, HashMap<String, String>> services = RentalServiceConfig
					.getServices();
			for (String rentalServiceId : services.keySet()) {
				System.out
						.println("Creating RentalService: " + rentalServiceId);
				ActorRef r = getContext().actorOf(
						Props.create(new RentalServiceCreator()));
				getContext().watch(r);
				routees.add(new ActorRefRoutee(r));
			}
			router = new Router(new BroadcastRoutingLogic(), routees);
		}
		if (message instanceof EdifactRequestEvent) {
			// Broadcast request to all rental services
			ActorSelection actor = this
					.context()
					.system()
					.actorSelection("akka://EventSimulator/user/eventCollector");
			actor.tell(message, this.getSelf());
			router.route(message, this.getSelf());
		}
	}

	static class RentalServiceCreator<T extends RentalService> implements
			Creator<RentalService> {
		private static final long serialVersionUID = -6248618599821101457L;

		@Override
		public T create() {
			String rentalServiceId = RentalServiceConfig.getNextServiceId();
			return (T) new RentalService(rentalServiceId, RentalServiceConfig
					.getServices().get(rentalServiceId));

		}
	}
}