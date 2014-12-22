/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

/**
 * Define which type of topology strategy will be used
 *
 */
public enum TopologyType {
	RACK_AS_RACK, //original hadoop rack awareness topology
	HOST_AS_RACK, //treat each host as rack in the case of rack info missing
	HVE, //hadoop virtualization enhancement, refer to HADOOP-8468
	NONE
}
