/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
   public static final String CLUSTER_CONFIG_FORMAT_ERROR =
         "The format of configuration is invalid !";
   public static final String CLUSTER_CONFIG_TYPE_ERROR =
         "The configuration is invalid ! At present, we only support ";
   public static final String LIST_CONFIG_ERROR =
         "The blacklist or whitelist config error!";
   public static final String PRACK_NO_TOPOLOGY_TYPE_SPECIFIED =
         "Rack Policy in spec file will not benefit Hadoop running when not specify topology through CLI. By default, Hadoop cluster will use NONE topology.";
   public static final String FAIR_SCHEDULER_FILE_NAME = "fair-scheduler.xml";
   public static final String FAIR_SCHEDULER_FILE_ATTRIBUTE = "text";
   public static final String SET_MANUAL_ELASTICITY_INFO_VERSION = "version";
   public static final String SET_MANUAL_ELASTICITY_INFO_CLUSTER_NAME = "cluster_name";
   public static final String SET_MANUAL_ELASTICITY_INFO_JOBTRACKER = "jobtracker";
   public static final String SET_MANUAL_ELASTICITY_INFO_INSTANCE_NUM = "instance_num";
   public static final String SET_MANUAL_ELASTICITY_INFO_NODE_GROUPS = "node_groups";
   public static final String SET_MANUAL_ELASTICITY_INFO_SERENGETI_INSTANCE =
         "serengeti_instance";
   public static final String CLUSTER_NAME_PATTERN = "[a-zA-Z0-9_]+";
   public static final String NODE_GROUP_NAME_PATTERN = "[a-zA-Z0-9]+";
   public static final String NAME_PATTERN = "[a-zA-Z0-9_\\-\\s]+";
   public static final String SERENGETI_SERVER_VM_MOBID = "vim.cms_moref";
   public static final String GUEST_VARIABLE_DNS_KEY_1 = "dnsserver1";
   public static final String GUEST_VARIABLE_DNS_KEY_0 = "dnsserver0";
   public static final String GUEST_VARIABLE_HOSTNAME_KEY = "hostname";
   public static final String GUEST_VARIABLE_GATEWAY_KEY = "gateway";
   public static final String GUEST_VARIABLE_NETMASK_KEY = "netmask";
   public static final String GUEST_VARIABLE_IP_KEY = "ipaddr";
   public static final String GUEST_VARIABLE_POLICY_KEY = "bootproto";
   public static final String NODE_ACTION_CLONING_VM = "Cloning VM";
   public static final String NODE_ACTION_CLONING_FAILED = "Cloning VM failed";
   public static final String NODE_ACTION_WAITING_IP = "Waiting for ip";
   public static final String NODE_ACTION_GET_IP_FAILED =
         "Failed to get VM ip address";
   public static final int VM_POWER_ON_WAITING_SEC = 600;
   public static final int VM_SHUTDOWN_WAITING_SEC = 600;
   public static final String PORT_GROUP_NAME_PATTERN =
         "[a-zA-Z0-9_\\-\\s\\.]+";
   public static final String VC_DATASTORE_NAME_PATTERN =
         "[a-zA-Z0-9_\\-\\s\\?\\*\\.\\(\\)]+";
   public static final String MULTI_INPUTS_CHECK =
         " cannot be empty, please check it.";
   public static final String WRONG_NUM_OF_JOBTRACKER =
         "the number of jobtrackers is more than 1";
   public static final String WRONG_NUM_OF_RESOURCEMANAGER =
         "the number of resourcemanagers is more than 1";
   public static final String WRONG_NUM_OF_ZOOKEEPER =
         "the number of zookeepers is less than 3";
   public static final String ODD_NUM_OF_ZOOKEEPER =
         "the number of zookeeper nodes is better to be odd numbers";
   public static final String WORKER_CLIENT_HA_FLAG =
         "the worker or client node group has HA enabled";
   public static final String REGISONSERVER_DATANODE_SEPERATION =
         "RegionServer and Datanode are separated, which will impact performance";
   public static final String NOT_DEFINED_ROLE =
         "Some roles are not identified";
   public static final String WRONG_NUM_OF_JOURNALNODE =
         "the number of journal nodes is less than 3 for namenode HA. It should be at least 3";
   public static final String ODD_NUM_OF_JOURNALNODE =
         "the number of journal nodes is better to be odd numbers";
   public static final String NAMENODE_AUTO_FAILOVER_ZOOKEEPER =
         "please configure a zookeeper group in order to support name node HA";
   public static final String NO_NAMENODE_HA =
         "no namenode HA, but journal nodes appear";
   public static final String WRONG_NUM_OF_NODEGROUPS =
         "the node groups including jobtracker, zookeeper or hmaster is larger than 1";
   public static final String WRONG_NUM_OF_WORKERNODES = "no worker exists";
   public static final String DATA_CLIENT_NODE_JOURNALNODE_COEXIST =
         "journal nodes are not allowed to be on data nodes or client nodes";
   public static final String STORAGE_TYPE_ALLOWED =
         "The allowed storage types include SHARED, LOCAL, and TEMPFS";
   public static final String TEMPFS_NOT_ALLOWED =
         "the storage type of TEMPFS can only be set on compute nodes with strict association to data nodes";
   public static final String DEFAULT_VENDOR = "Apache";
   public static final String CDH_VENDOR = "CDH";
   public static final String GPHD_VENDOR = "GPHD";
   public static final String HDP_VENDOR = "HDP";
   public static final String MAPR_VENDOR = "MAPR";
   public static final String CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2 =
         "HA and federation feature can not be supported in current distro";
   public static final String CDH4_1_PATTERN = "4\\.1(\\.\\d){0,2}";

   // rabbitMQ message format
   public static final String ERROR_CODE_FIELD = "error_code";
   public static final String ERROR_MSG_FIELD = "error_msg";
   public static final String FINISH_FIELD = "finished";
   public static final String SUCCEED_FIELD = "succeed";
   public static final String PROGRESS_FIELD = "progress";
   public static final String PROGRESS_MESSAGE_FIELD = "progress_msg";
   public static final String CLUSTER_DATA_FIELD = "cluster_data";
   public static final String CLUSTER_NAME_FIELD = "name";
   public static final String GROUP_FIELD = "groups";
   public static final String GROUP_NAME_FIELD = "name";
   public static final String INSTANCE_FIELD = "instances";
   public static final String INSTANCE_NAME_FIELD = "name";
   public static final String INSTANCE_STATUS_FIELD = "status";
   public static final String INSTANCE_ACTION_FIELD = "action";
   public static final String INSTANCE_IP_FIELD = "ip_address";

   // cloud provider
   public static final String CLOUD_PROVIDER_NAME_FIELD = "name";
   public static final String CLOUD_PROVIDER_TYPE_FIELD = "type";
   public static final String VSPHERE_PROVIDER_NAME = "static";
   public static final String VSPHERE_PROVIDER_TYPE = "VC";
   public static final String HA_FLAG_ON = "on";
   public static final String HA_FLAG_FT = "ft";

   private static final String ELASTIC_RUNTIME_AUTOMATION_ENABLE =
         "elastic_runtime.automation.enable";
}
