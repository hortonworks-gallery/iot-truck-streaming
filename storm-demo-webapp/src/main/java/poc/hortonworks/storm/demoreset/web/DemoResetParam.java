package poc.hortonworks.storm.demoreset.web;

import java.io.Serializable;

public class DemoResetParam implements Serializable {

	private static final long serialVersionUID = 9098078673788539467L;

	private boolean truncateHbaseTables;

	public boolean isTruncateHbaseTables() {
		return truncateHbaseTables;
	}

	public void setTruncateHbaseTables(boolean truncateHbaseTables) {
		this.truncateHbaseTables = truncateHbaseTables;
	}
	
	
}
