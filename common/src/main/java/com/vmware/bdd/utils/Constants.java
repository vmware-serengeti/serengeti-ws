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
   public static final String CLUSTER_CONFIG_FORMAT_ERROR = "The format of configuration is invalid !";
   public static final String CLUSTER_CONFIG_TYPE_ERROR = "The configuration is invalid ! At present, we only support ";
   public static final String LIST_CONFIG_ERROR = "The blacklist or whitelist config error!";
   public static final String PRACK_NO_TOPOLOGY_TYPE_SPECIFIED = "Rack Policy in spec file will not benefit Hadoop running when not specify topology through CLI. By default, Hadoop cluster will use NONE topology.";
   public static final String FAIR_SCHEDULER_FILE_NAME = "fair-scheduler.xml";
   public static final String FAIR_SCHEDULER_FILE_ATTRIBUTE = "text";
   public static final String CLUSTER_LIMIT_INFO_VERSION = "version";
   public static final String CLUSTER_LIMIT_INFO_CLUSTER_NAME = "cluster_name";
   public static final String CLUSTER_LIMIT_INFO_JOBTRACKER = "jobtracker";
   public static final String CLUSTER_LIMIT_INFO_INSTANCE_NUM = "instance_num";
   public static final String CLUSTER_LIMIT_INFO_NODE_GROUPS = "node_groups";
   public static final String CLUSTER_LIMIT_INFO_SERENGETI_INSTANCE = "serengeti_instance";
   public static final String CLUSTER_NAME_PATTERN = "[a-zA-Z0-9_]+";
   public static final String NODE_GROUP_NAME_PATTERN = "[a-zA-Z0-9_\\s]+";
   public static final String NAME_PATTERN = "[a-zA-Z0-9_\\-\\s]+";
   public static final String VC_DATASTORE_NAME_PATTERN = "[a-zA-Z0-9_\\-\\s\\?\\*]+";
   public static final String MULTI_INPUTS_CHECK = " cannot be empty, please check it.";
   public static final String WRONG_NUM_OF_JOBTRACKER = "the number of jobtrackers is more than 1";
   public static final String WRONG_NUM_OF_ZOOKEEPER = "the number of zookeepers is less than 3";
   public static final String ODD_NUM_OF_ZOOKEEPER = "the number of zookeeper nodes is better to be odd numbers";
   public static final String WORKER_CLIENT_HA_FLAG = "the worker or client node group has HA enabled";
   public static final String REGISONSERVER_DATANODE_SEPERATION = "RegionServer and Datanode are separated, which will impact performance";
   public static final String NOT_DEFINED_ROLE = "Some roles are not identified";
   public static final String WRONG_NUM_OF_JOURNALNODE = "the number of journal nodes is less than 3 for namenode HA. It should be at least 3";
   public static final String ODD_NUM_OF_JOURNALNODE = "the number of journal nodes is better to be odd numbers";
   public static final String NAMENODE_AUTO_FAILOVER_ZOOKEEPER = "please configure a zookeeper group in order to support name node HA";
   public static final String NO_NAMENODE_HA = "no namenode HA, but journal nodes appear";
   public static final String WRONG_NUM_OF_NODEGROUPS = "the node groups including jobtracker, zookeeper or hmaster is larger than 1";
   public static final String WRONG_NUM_OF_WORKERNODES = "no worker exists";
   public static final String DATA_CLIENT_NODE_JOURNALNODE_COEXIST = "journal nodes are not allowed to be on data nodes or client nodes";
   public static final String TEMPFS_NOT_ALLOWED = "the disk type of TEMPFS can only be set on compute nodes with strict association to data nodes";
   public static final String DEFAULT_VENDOR = "Apache";
   public static final String CLOUDERA_VENDOR = "Cloudera";
   public static final String CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2 = "HA and federation feature can not be supported in current distro";
   public static final String CDH4_1_PATTERN = "4\\.1(\\.\\d){0,2}";
}
