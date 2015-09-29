package com.hortonworks.streaming.impl.domain.rental;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hortonworks.streaming.impl.domain.Event;

/**
 * @author Paul Codding <paul@hortonworks.com>
 * 
 */
public class EdifactRequestEvent extends Event {
	private long transactionId;
	// PHX
	private String airportCode;
	// 140221
	private String pickupDate;
	// 1200
	private String pickupTime;
	// 140303
	private String returnDate;
	// 1200
	private String returnTime;
	// 07560685
	private String iata;

	public EdifactRequestEvent(long transactionId, String airportCode,
			String pickupDate, String pickupTime, String returnDate,
			String returnTime, String iata) {
		this.transactionId = transactionId;
		this.airportCode = airportCode;
		this.pickupDate = pickupDate;
		this.pickupTime = pickupTime;
		this.returnDate = returnDate;
		this.returnTime = returnTime;
		this.iata = iata;
	}

	@Override
	public String toString() {
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS");
		String requestDate = format.format(now);

		return requestDate
				+ ",Availability Request,/GDS-1P/SOURCE-ET/TARGET-ET/TX#-"
				+ transactionId
				+ "/ACTION-SH/LOYALTY#-/CONTRACT#-/ECHO-3AXXA00000002461382543870000001673115557756181P18ET03_________________	V.<cr>VDDG.W./E1ETCS/I11PCS/P000222<cr>VGZ.<cr>UIB+UNOA:4+063FBE++++1P+ET+131023:1557'UIH+AVLREQ:D:97B::UN++063FBE+:F+131023:1557'MSD+2:37'ORG+1P:HDQ+"
				+ iata + ":OMW+HVN:CT++1+US+RS'PLI+176:C96'TVL+" + pickupDate
				+ ":" + pickupTime + ":" + returnDate + ":" + returnTime + "+"
				+ airportCode + "*" + airportCode
				+ "C96+ET'UIT++6'UIZ+063FBE'+ ";
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public String getAirportCode() {
		return airportCode;
	}

	public void setAirportCode(String airportCode) {
		this.airportCode = airportCode;
	}

	public String getPickupDate() {
		return pickupDate;
	}

	public void setPickupDate(String pickupDate) {
		this.pickupDate = pickupDate;
	}

	public String getPickupTime() {
		return pickupTime;
	}

	public void setPickupTime(String pickupTime) {
		this.pickupTime = pickupTime;
	}

	public String getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(String returnDate) {
		this.returnDate = returnDate;
	}

	public String getReturnTime() {
		return returnTime;
	}

	public void setReturnTime(String returnTime) {
		this.returnTime = returnTime;
	}

	public String getIata() {
		return iata;
	}

	public void setIata(String iata) {
		this.iata = iata;
	}
}