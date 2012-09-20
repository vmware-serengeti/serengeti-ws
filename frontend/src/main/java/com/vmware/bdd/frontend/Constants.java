/*****************************************************************************
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
 ****************************************************************************/
package com.vmware.bdd.frontend;

public class Constants {

   public static final String REST_CREATE_OP = "create";
   public static final String REST_GET_OP = "get";
   public static final String REST_GET_ALL_OP = "getAll";

   public static final String REST_PATH_CLUSTER = "cluster";
   public static final String REST_PATH_CLUSTERS = "clusters";
   public static final String REST_PATH_RESOURCEPOOLS = "resourcepools";
   public static final String REST_PATH_RESOURCEPOOL = "resourcepool";
   public static final String REST_PATH_DATASTORES = "datastores";
   public static final String REST_PATH_DATASTORE = "datastore";
   public static final String REST_PATH_NODEGROUP = "nodegroup";
   public static final String REST_PATH_NODE = "node";
   public static final String REST_PATH_TASK = "task";
   public static final String REST_PATH_LOGIN = "j_spring_security_check";
   public static final String REST_PATH_LOGOUT = "j_spring_security_logout";
   public static final String REST_PATH_NETWORK = "network";
   public static final String REST_PATH_NETWORKS = "networks";
   public static final String REST_PATH_DISTROS = "distros";
   public static final String REST_PATH_DISTRO = "distro";
   public static final String REST_PATH_CONFIG = "config";
   public static final String REST_PATH_SPEC = "spec";

   public static final String QUERY_ACTION_KEY = "state";
   public static final String QUERY_ACTION_START = "start";
   public static final String QUERY_ACTION_STOP = "stop";
   public static final String QUERY_ACTION_RESUME = "resume";
   public static final String QUERY_DETAIL = "?details=true";

   public static final String HTTP_VERB_ERROR = "Mismatched http verb";

   public static final String CREATE_SUCCESS = "Succeeded";
   public static final String CREATE_FAILURE = "Failed";
   public static final String GET_FAILURE = "Cannnot get";
   public static final String DELETE_SUCCESS = "is deleted";
   public static final String DELETE_FAILURE = "Cannnot delete";
   public static final String UPDATE_SUCCESS = "is updated";
   public static final String UPDATE_FAILURE = "Cannnot update";
   public static final String ACTION_OPS_SUCCESS = "Succeeded to";
   public static final String ACTION_OPS_FAILURE = "Unable to";
   public static final String CONNECT_SUCCESS = "Connected";
   public static final String CONNECT_FAILURE = "Cannot connect to the host";
   public static final String DISCONNECT_FAILURE = "Disconnect failed!";
   public static final String NEED_CONNECTION = "no connection to serengeti server, please connect first.";

   public static final String OUTPUT_INDENT = "  ";
   public static final String OUTPUT_SPLITTER = ", ";
   public static final String OUTPUT_OBJECT_CLUSTER = "cluster";
   public static final String OUTPUT_OBJECT_NODES_IN_CLUSTER = "nodes in cluster";
   public static final String OUTPUT_OBJECT_DATASTORE = "datastore";
   public static final String OUTPUT_OBJECT_DISTRO = "distro";
   public static final String OUTPUT_OBJECT_NETWORK = "network";
   public static final String OUTPUT_OBJECT_NODE = "node";
   public static final String OUTPUT_OBJECT_RESOURCEPOOL = "resourcepool";
   public static final String OUTPUT_OP_CREATE = "create";
   public static final String OUTPUT_OP_CONFIG = "config";
   public static final String OUTPUT_OP_EXPORT = "export";
   public static final String OUTPUT_OP_ADD = "add";
   public static final String OUTPUT_OP_LIST = "list";
   public static final String OUTPUT_OP_TARGET = "target";
   public static final String OUTPUT_OP_DELETE = "delete";
   public static final String OUTPUT_OP_START = "start";
   public static final String OUTPUT_OP_STOP = "stop";
   public static final String OUTPUT_OP_RESIZE = "resize";
   public static final String OUTPUT_OP_RESUME = "resume";
   public static final String OUTPUT_OP_RESULT_FAIL = "failed";
   public static final String OUTPUT_OP_RESULT_CREAT = "created";
   public static final String OUTPUT_OP_RESULT_CONFIG = "configured";
   public static final String OUTPUT_OP_RESULT_DELETE = "deleted";
   public static final String OUTPUT_OP_RESULT_START = "started";
   public static final String OUTPUT_OP_RESULT_STOP = "stopped";
   public static final String OUTPUT_OP_RESULT_RESIZE = "resized";
   public static final String OUTPUT_OP_RESULT_RESUME = "resumed";
   public static final String OUTPUT_OP_RESULT_ADD = "added";
   public static final String OUTPUT_OP_NODEGROUP_MISSING = "must specify nodegroup name when node name is specified";
   public static final String OUTPUT_UNKNOWN_HOST = "unknown host";
   public static final String OUTPUT_HOST_REFUSE = "Connection refused. Please check the port number.";
   public static final String OUTPUT_HOST_TIMEOUT = "connection timeout";
   public static final String OUTPUT_LOG_INFO = "you can get task failure details from serengeti server log at: ";

   public static final String HTTP_CONNECTION_PREFIX = "http://";
   public static final String HTTP_CONNECTION_SUFFIX = "/serengeti/api/";
   public static final String HTTP_CONNECTION_API = "api/";
   public static final String HTTP_CONNECTION_LOGIN_SUFFIX = "/serengeti/";
   public static final String PROPERTY_FILE = "cli.properties";
   public static final String PROPERTY_HOST = "host";
   public static final String PROPERTY_FILE_HOST_COMMENT = "update host information";
   public static final String PROPERTY_FILE_HOST_FAILURE = "cannot update the host information";

   public static final String INPUT_SPEC_PARAM = "\"spec\"";
   public static final String INPUT_RPNAMES_PARAM = "\"rpNames\"";
   public static final String INPUT_DSNAMES_PARAM = "\"dsNames\"";
   public static final String MULTI_INPUTS_CHECK = " cannot be empty, please check it.";
   public static final String INPUT_INSTANCE_NUM_PARAM = "\"instanceNum\"";
   public static final String INTEGER_INPUTS_CHECK = " should be larger than existed, please check it.";
   public static final String INVALID_VALUE = "invalid value";

   public static final String PARAMS_EXCLUSION = " cannot be set at the same time.";

   public static final String PARAMS_EXCLUSION_PAIR_NETWORK_ADD_IP_DHCP = "ip and dhcp";
   public static final String PARAMS_NETWORK_ADD_IP_DHCP_NOT_NULL = "You must enter one of ip,dhcp.";
   public static final String PARAMS_NETWORK_ADD_DNS = "dns";
   public static final String PARAMS_NETWORK_ADD_GATEWAY = "gateway";
   public static final String PARAMS_NETWORK_ADD_MASK = "mask";
   public static final String PARAMS_NETWORK_ADD_OR = " or ";
   public static final String PARAMS_NETWORK_ADD_FORMAT_ERROR =
         " valid format is ips or ip ranges, eg. 255.0.0.1-100, 255.0.0.1 or 255.0.3.1-200,255.0.0.1-255";
   public static final String PARAMS_NETWORK_ADD_IP_ERROR = " please provide valid ip or mask format.";
   public static final String PARAM_NOT_SUPPORTED = " input cannnot be accepted, we only support: ";
   public static final String ROLE_HADOOP_NAME_NODE = "hadoop_namenode";
   public static final String ROLE_HADOOP_JOB_TRACKER = "hadoop_jobtracker";
   public static final String ROLE_HADOOP_DATANODE = "hadoop_datanode";
   public static final String ROLE_HADOOP_TASKTRACKER = "hadoop_tasktracker";
   public static final String ROLE_HADOOP_CLIENT = "hadoop_client";
   public static final String ROLE_HIVE = "hive";
   public static final String ROLE_HIVE_SERVER = "hive_server";
   public static final String ROLE_PIG = "pig";
   public static final String PARAM_CLUSTER_WARNING =
         "Warning: Serengeti supports multi-instance of tasktracker and datanode, only one instance of namenode and jobtracker, multi-instance of client. Only namenode and jobtracker instance support to configure haFlag.";
   public static final String PARAM_CLUSTER_FAILED_INSTANCE_NUM_NEGATIVE = "instanceNum can not be negative";
   public static final String PARAM_CLUSTER = "cluster name";
   public static final String PARAM_DISTRO = "distro name";
   public static final String PARAM_NETWORK_NAME = "network name";
   public static String PARAM_NOT_SPECIFIED = " is not specified, please check it.";
   public static String PARAM_NOT_EXISTED = " cannot be found. Please add it first.";
   public static String PARAM_NOT_CONTAIN_HORIZONTAL_LINE = " cannot contain '-'.";

   public static int FORMAT_COLUMN_DISTANCE = 2;
   public static String FORMAT_TABLE_COLUMN_NAME = "NAME";
   public static String FORMAT_TABLE_COLUMN_STATUS = "STATUS";
   public static String FORMAT_TABLE_COLUMN_IP = "IP";
   public static String FORMAT_TABLE_COLUMN_ROLES = "ROLES";
   public static String FORMAT_TABLE_COLUMN_INSTANCE = "INSTANCE";
   public static String FORMAT_TABLE_COLUMN_CPU = "CPU";
   public static String FORMAT_TABLE_COLUMN_MEM = "MEM(MB)";
   public static String FORMAT_TABLE_COLUMN_TYPE = "TYPE";
   public static String FORMAT_TABLE_COLUMN_SIZE = "SIZE(GB)";
   public static String FORMAT_TABLE_COLUMN_HOST = "HOST";
   public static String FORMAT_TABLE_COLUMN_SPACE = "SPACE(GB)";
   public static String FORMAT_TABLE_COLUMN_FREE_SPACE = "FREE_SPACE(GB)";
   public static String FORMAT_TABLE_COLUMN_PORT_GROUP = "PORTGROUP";
   public static String FORMAT_TABLE_COLUMN_FREE_IPS = "FREE_IPS";
   public static String FORMAT_TABLE_COLUMN_ASSIGNED_IPS = "ASSIGNED_IPS";
   public static String FORMAT_TABLE_COLUMN_DNS1 = "DNS1";
   public static String FORMAT_TABLE_COLUMN_DNS2 = "DNS2";
   public static String FORMAT_TABLE_COLUMN_GATEWAY = "GATEWAY";
   public static String FORMAT_TABLE_COLUMN_MASK = "MASK";
   public static String FORMAT_TABLE_COLUMN_NODE_NAME = "NODE";
   public static String FORMAT_TABLE_COLUMN_NODE_GROUP_NAME = "NODEGROUP";
   public static String FORMAT_TABLE_COLUMN_CLUSTER_NAME = "CLUSTER";
   public static String FORMAT_TABLE_COLUMN_PATH = "PATH";
   public static String FORMAT_TABLE_COLUMN_RAM_MB = "RAM(MB)";
   public static String FORMAT_TABLE_COLUMN_CPU_MHZ = "CPU(MHZ)";
   public static final String FORMAT_TABLE_COLUMN_TASK = "TASK";
   public static final String DEFAULT_DISTRO = "apache";
   public static final String PARAM_NO_DISTRO_AVAILABLE = "no distro available,please upload one distro";
   public static final String PARAM_PROMPT_CONTINUE_MESSAGE = "Are you sure you want to continue (Y/N)? ";
   public static final String PARAM_CLUSTER_IN_BLACK_LIST_WARNING =
         "not allowed to customize in Serengeti. Your settings will not take effect.";
   public static final String PARAM_CLUSTER_NOT_IN_WHITE_LIST_WARNING =
         "not regular configuration properties in Apache Hadoop. ";
   public static final String PARAM_CLUSTER_CONFIG_RUNNING_JOB_WARNING =
         "Applying new Hadoop configuration will restart Hadoop processes and may lead to termination of running jobs.";
   public static final String PARAM_CLUSTER_SPEC_HA_ERROR = "The ha property must be one of 'off', 'on' and 'ft' in spec file ";
   public static final String CONNECT_UNAUTHORIZATION = "The connection is refused, may be invalid password or overtime session, please try to reconnect!";
   public static final String CONNECT_USER_NAME = "The user name";
   public static final String CONNECT_PASSWORD = "The password";
   public static final String CONNECT_CAN_NOT_BE_NULL = " can not be null!";
   public static final String CONNECT_ENTER_USER_NAME = "Please enter the username: ";
   public static final String CONNECT_ENTER_PASSWORD = "Please enter the password: ";
   public static final String CONNECT_CHECK_LOGIN = "You are not login, please run connect first!";
   public static final String CONNECT_SESSION_TIME_OUT = "Session timeout, please run connect again!";
   public static final String OUTPUT_OBJECT_CONNECT = "connect";
}
