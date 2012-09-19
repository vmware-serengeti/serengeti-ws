/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.utils;


public class Constants {
   public static final String CLUSTER_CONFIG_ERROR="The configuration of spec file is invalid!";
   public static final String PRACK_NO_TOPOLOGY_TYPE_SPECIFIED = "Rack Policy in spec file will not benefit Hadoop running when not specify topology through CLI. By default, Hadoop cluster will use NONE topology.";
   public static final String PRACK_WITH_SHARED_STORAGE = "Cluster PRack Policy will not take effect when node group uses SHARED storage.";
   public static final String PRACK_WITH_STRICT_ASSOCIATION = "Cluster PRack Policy will not take effect when compute node group uses STRICT group association.";
   public static final String PRACK_SAME_RACK_WITH_WRONG_VALUES = "There is no proper rack to place your nodes. Please \n(1) Add more hosts in vSphere cluster into PRack-PHost mapping file; \n(2)Adjust cluster PRack policy.";
   public static final String FAIR_SCHEDULER_FILE_NAME = "fair-scheduler.xml";
   public static final String FAIR_SCHEDULER_FILE_ATTRIBUTE = "text";
   public static final String CLUSTER_LIMIT_INFO_VERSION = "version";
   public static final String CLUSTER_LIMIT_INFO_CLUSTER_NAME = "cluster_name";
   public static final String CLUSTER_LIMIT_INFO_JOBTRACKER = "jobtracker";
   public static final String CLUSTER_LIMIT_INFO_INSTANCE_NUM = "instance_num";
   public static final String CLUSTER_LIMIT_INFO_NODE_GROUPS = "node_groups";
   public static final String CLUSTER_LIMIT_INFO_SERENGETI_INSTANCE = "serengeti_instance";
}
