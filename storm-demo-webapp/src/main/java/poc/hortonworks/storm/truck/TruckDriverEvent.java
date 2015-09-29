package poc.hortonworks.storm.truck;

public class TruckDriverEvent {

	private String truckDriverEventKey;
	
	private int driverId;
	private int truckId;
	private String timeStampString;
	private double longitude;
	private double latitude;
	private String infractionEvent;
	private int numberOfInfractions;
	
	
	public TruckDriverEvent(int driverId, int truckId, String timeStampString,
			double longitude, double latitude, String infractionEvent,
			int numberOfInfractions) {
		super();
		this.driverId = driverId;
		this.truckId = truckId;
		this.setTruckDriverEventKey(constructKey());
		this.timeStampString = timeStampString;
		this.longitude = longitude;
		this.latitude = latitude;
		this.infractionEvent = infractionEvent;
		this.numberOfInfractions = numberOfInfractions;
	}


	private String constructKey() {
		return this.driverId + "|" + truckId;
	}


	public int getDriverId() {
		return driverId;
	}


	public void setDriverId(int driverId) {
		this.driverId = driverId;
	}


	public int getTruckId() {
		return truckId;
	}


	public void setTruckId(int truckId) {
		this.truckId = truckId;
	}


	public String getTimeStampString() {
		return timeStampString;
	}


	public void setTimeStampString(String timeStampString) {
		this.timeStampString = timeStampString;
	}


	public double getLongitude() {
		return longitude;
	}


	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}


	public double getLatitude() {
		return latitude;
	}


	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getInfractionEvent() {
		return infractionEvent;
	}


	public void setInfractionEvent(String infractionEvent) {
		this.infractionEvent = infractionEvent;
	}


	public int getNumberOfInfractions() {
		return numberOfInfractions;
	}


	public void setNumberOfInfractions(int numberOfInfractions) {
		this.numberOfInfractions = numberOfInfractions;
	}


	public String getTruckDriverEventKey() {
		return truckDriverEventKey;
	}


	public void setTruckDriverEventKey(String truckDriverEventKey) {
		this.truckDriverEventKey = truckDriverEventKey;
	}
	
	
	
	
	

}
