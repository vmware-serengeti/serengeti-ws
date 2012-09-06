package com.vmware.bdd.apitypes;

import java.util.List;
import java.util.Map;

/**
 * Keep <rack_name, hosts> pairs
 *
 */
public class RackInfo {
	private Map<String, List<String>> racks;

	public Map<String, List<String>> getRacks() {
		return racks;
	}

	public void setRacks(Map<String, List<String>> racks) {
		this.racks = racks;
	}
}
