package com.hortonworks.streaming.impl.domain.rental;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import com.hortonworks.streaming.impl.domain.Event;

public class EdifactResponseEvent extends Event {
	private EdifactRequestEvent request = null;
	private long transactionId;
	private HashMap<String, String> serviceConfig = null;
	double baseRate = 0.0;
	double carClassMultiplier = 0;
	double airportPremium = 0.0;

	public EdifactResponseEvent(EdifactRequestEvent request,
			HashMap<String, String> serviceConfig) {
		this.transactionId = request.getTransactionId();
		this.request = request;
		this.serviceConfig = serviceConfig;
		String baseRateAsString = serviceConfig.get("baseRate");
		String carClassMultiplierAsString = serviceConfig
				.get("carClassMultiplier");
		airportPremium = hashAirport(request.getAirportCode());
		baseRate = Double.parseDouble(baseRateAsString);
		carClassMultiplier = Double.parseDouble(carClassMultiplierAsString);
	}

	public EdifactResponseEvent(EdifactRequestEvent request,
			HashMap<String, String> serviceConfig, boolean changeRate) {
		this(request, serviceConfig);
		if (changeRate == true)
			baseRate += (2 + new Random().nextInt(10));
	}

	@Override
	public String toString() {
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd,HH:mm:ss.SSS");
		String responseDate = format.format(now);
		return responseDate
				+ ",Availability Response,/GDS-1P/SOURCE-ET/TARGET-ET/TX#-"
				+ transactionId
				+ "__"
				+ serviceConfig.get("companyName")
				+ "/ACTION-SH/LOYALTY#-/CONTRACT#-/ECHO-3AXXA00000002461382543870000001673115557756181P18ET03_________________	V.<cr>VDDG.W./E1ETCS/I11PCS/P000222<cr>VGZ.<cr>UIB+UNOA:4+063FBE:::54460001++++ET+1P+131023:1557'UIH+AVLRSP:D:97B::UN++063FBE:::54460001++131023:1557'MSD+2:37+1'PLI+J02:122*176:"
				+ request.getAirportCode()
				+ "C96'PRD+:ECAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("ECAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("ECAR")[1]
				+ "++166::UNL*8:65.32*79::UNL*9:21.78*81::UNL'PRD+:CCAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("CCAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("CCAR")[1]
				+ "++166::UNL*8:70.35*79::UNL*9:23.46*81::UNL'PRD+:ICAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("ICAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("ICAR")[1]
				+ "++166::UNL*8:78.39*79::UNL*9:26.14*81::UNL'PRD+:SCAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("SCAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("SCAR")[1]
				+ "++166::UNL*8:85.42*79::UNL*9:28.48*81::UNL'PRD+:FCAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("FCAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("FCAR")[1]
				+ "++166::UNL*8:95.47*79::UNL*9:31.83*81::UNL'PRD+:PCAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("PCAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("PCAR")[1]
				+ "++166::UNL*8:126.63*79::UNL*9:42.22*81::UNL'PRD+:LCAR'PDT++:4'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("LCAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("LCAR")[1]
				+ ":::17++166::UNL*8:152.76*79::UNL*9:50.93*81::UNL'PRD+:PXAR'PDT++:4'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("PXAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("PXAR")[1]
				+ ":::17++166::UNL*8:126.63*79::UNL*9:42.22*81::UNL'PRD+:MVAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("MVAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("MVAR")[1]
				+ "++166::UNL*8:150.75*79::UNL*9:50.26*81::UNL'PRD+:FVAR'PDT++:4'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("FVAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("FVAR")[1]
				+ ":::17++33:::1050*31:.20*8:146.73*79:::150*9:48.92*81:::0'PRD+:IFAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("IFAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("IFAR")[1]
				+ "++166::UNL*8:100.50*79::UNL*9:33.51*81::UNL'PRD+:SFAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("SFAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("SFAR")[1]
				+ "++166::UNL*8:110.55*79::UNL*9:36.86*81::UNL'PRD+:FFAR'PDT++:4'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("FFAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("FFAR")[1]
				+ ":::17++166::UNL*8:180.90*79::UNL*9:60.31*81::UNL'PRD+:PFAR'PDT++:4'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("PFAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("PFAR")[1]
				+ ":::17++166::UNL*8:201.00*79::UNL*9:67.01*81::UNL'PRD+:SPAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("SPAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("SPAR")[1]
				+ "++166::UNL*8:100.50*79::UNL*9:33.51*81::UNL'PRD+:PPAR'PDT++:5'RTC+ETBEST'TFF+R:"
				+ calculateCostForCarClass("PPAR")[0]
				+ ":USD:WY:1:::"
				+ calculateCostForCarClass("PPAR")[1]
				+ "++166::UNL*8:160.80*79::UNL*9:53.61*81::UNL'UIT++68'UIZ+063FBE:::54460001'+ ";
	}

	private double hashAirport(String airportCode) {
		char ch[];
		ch = airportCode.toCharArray();

		int i, sum;
		for (sum = 0, i = 0; i < airportCode.length(); i++)
			sum += ch[i];
		return (sum - 200) / 100.0;
	}

	private String[] calculateCostForCarClass(String carClass) {
		String[] prices = new String[2];
		double dailyPrice = baseRate;

		if (carClass.equals("ECAR")) {
			carClassMultiplier += 0;
		} else if (carClass.equals("CCAR")) {
			carClassMultiplier += .6;
		} else if (carClass.equals("ICAR")) {
			carClassMultiplier += .8;
		} else if (carClass.equals("SCAR")) {
			carClassMultiplier += .9;
		} else if (carClass.equals("FCAR")) {
			carClassMultiplier += 1;
		} else if (carClass.equals("PCAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("LCAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("PXAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("MVAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("FVAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("IFAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("SFAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("FFAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("PFAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("SPAR")) {
			carClassMultiplier += 1.5;
		} else if (carClass.equals("PPAR")) {
			carClassMultiplier += 1.5;
		}
		dailyPrice += dailyPrice * (carClassMultiplier + airportPremium);
		prices[0] = new DecimalFormat("#.00").format(dailyPrice);
		prices[1] = new DecimalFormat("#.00").format(dailyPrice * 7);
		return prices;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
}