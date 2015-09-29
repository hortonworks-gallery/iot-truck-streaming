package poc.hortonworks.storm.truck;

import java.util.Collection;

import poc.hortonworks.domain.transport.TruckDriverViolationEvent;

public class DriverEventsResponse {
	
	private Collection<TruckDriverViolationEvent> violationEvents;
	private double startLat;
	private double startLong;
	private int zoomLevel;
	private int truckSymbolSize;
	
	public Collection<TruckDriverViolationEvent> getViolationEvents() {
		return violationEvents;
	}
	public void setViolationEvents(
			Collection<TruckDriverViolationEvent> violationEvents) {
		this.violationEvents = violationEvents;
	}
	public double getStartLat() {
		return startLat;
	}
	public void setStartLat(double startLat) {
		this.startLat = startLat;
	}
	public double getStartLong() {
		return startLong;
	}
	public void setStartLong(double startLong) {
		this.startLong = startLong;
	}
	public int getZoomLevel() {
		return zoomLevel;
	}
	public void setZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;
	}
	public int getTruckSymbolSize() {
		return truckSymbolSize;
	}
	public void setTruckSymbolSize(int truckSymbolSize) {
		this.truckSymbolSize = truckSymbolSize;
	}
	
	
	
}
