package com.vmware.bdd.apitypes;

/**
 * Define which type of topology strategy will be used
 *
 */
public enum TopologyType {
	RACK_HOST, //original hadoop rack awareness topology 
	HOST_AS_RACK, //treat each host as rack in the case of rack info missing
	HVE, //hadoop virtualization enhancement, refer to HADOOP-8468
	NONE
}
