/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.apitypes;

import java.util.Map;

/**
 * The topology information in hadoop
 *
 */
public class Topology {
	public enum TopologyType {
		RACK_HOST, HOST_AS_RACK, HVE, NONE 
	}

	//define which type of topology strategy will be used
	private TopologyType type;
	//keep <rack_name, hosts> pairs
	private Map<String, String[]> racks;

	public Map<String, String[]> getRacks() {
		return racks;
	}

	public void setRacks(Map<String, String[]> racks) {
		this.racks = racks;
	}

	public TopologyType getType() {
		return type;
	}

	public void setType(TopologyType type) {
		this.type = type;
	}
}