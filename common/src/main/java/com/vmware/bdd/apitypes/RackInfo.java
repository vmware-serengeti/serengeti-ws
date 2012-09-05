package com.vmware.bdd.apitypes;

import java.util.Map;

/**
 * Keep <rack_name, hosts> pairs
 *
 */
public class RackInfo {
	private Map<String, String[]> racks;

	public Map<String, String[]> getRacks() {
		return racks;
	}

	public void setRacks(Map<String, String[]> racks) {
		this.racks = racks;
	}
}
