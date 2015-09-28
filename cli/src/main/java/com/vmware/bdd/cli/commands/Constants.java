/*****************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.cli.commands;

public class Constants {

   public static final String REST_CREATE_OP = "create";
   public static final String REST_GET_OP = "get";
   public static final String REST_GET_ALL_OP = "getAll";

   public static final String REST_PATH_HELLO = "hello";
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
   public static final String REST_PATH_APPMANAGERS = "appmanagers";
   public static final String REST_PATH_APPMANAGER = "appmanager";
   public static final String REST_PATH_DISTROS = "distros";
   public static final String REST_PATH_DISTRO = "distro";
   public static final String REST_PATH_CONFIG = "config";
   public static final String REST_PATH_UPGRADE = "upgrade";
   public static final String REST_PATH_SYNC_PARAM = "param";
   public static final String REST_PATH_ASYNC_PARAM = "param_wait_for_result";
   public static final String REST_PATH_FIX = "fix";
   public static final String REST_PATH_FIX_DISK = "disk";
   public static final String REST_PATH_SPEC = "spec";
   public static final String REST_PATH_TOPOLOGY = "racks";
   public static final String REST_PATH_DEFAULT_DISTRO = "defaultdistro";
   public static final String REST_PATH_ROLES = "roles";
   public static final String REST_PATH_CONFIGURATIONS = "configurations";
   public static final String REST_PATH_CLUSTER_RACK = "rack";
   public static final String REST_PATH_CLUSTER_RACK_PARAM_TOPOLOGY = "topology";
   public static final String REST_PATH_TEMPLATES = "templates";

   public static final String QUERY_ACTION_KEY = "state";
   public static final String QUERY_ACTION_START = "start";
   public static final String QUERY_ACTION_STOP = "stop";
   public static final String QUERY_ACTION_RESUME = "resume";
   public static final String QUERY_DETAIL = "?details=true";

   public static final String HTTP_VERB_ERROR = "Mismatched http verb";

   public static final String CREATE_SUCCESS = "Succeeded";
   public static final String CREATE_FAILURE = "Failed";
   public static final String GET_FAILURE = "Cannot get";
   public static final String DELETE_SUCCESS = "is deleted";
   public static final String DELETE_FAILURE = "Cannot delete";
   public static final String UPDATE_SUCCESS = "is updated";
   public static final String UPDATE_FAILURE = "Cannot update";
   public static final String ACTION_OPS_SUCCESS = "Succeeded to";
   public static final String ACTION_OPS_FAILURE = "Unable to";
   public static final String CONNECT_SUCCESS = "Connected";
   public static final String CONNECTION_ALREADY_ESTABLISHED = "Already connected. No need to connect again.";
   public static final String CONNECT_FAILURE = "Cannot connect to the host";
   public static final String CONNECT_FAILURE_NO_SESSION_ID = "Cannot connect to the host: No session id returned.";
   public static final String UNSUPPORTED_HTTP_RESPONSE_CODE = "Unsupported HTTP Response Code: %1s";
   public static final String DISCONNECT_FAILURE = "Diconnect failed";
   public static final String NEED_CONNECTION = "no connection to serengeti server. You should connect it first.";

   public static final int SEPERATOR_LEN = 76;
   public static final String OUTPUT_INDENT = "  ";
   public static final String OUTPUT_SPLITTER = ", ";
   public static final String OUTPUT_OBJECT_CLUSTER = "cluster";
   public static final String OUTPUT_OBJECT_NODES_IN_CLUSTER = "Nodes in cluster";
   public static final String OUTPUT_OBJECT_DATASTORE = "datastore";
   public static final String OUTPUT_OBJECT_USERMGMT = "AD/LDAP server";
   public static final String OUTPUT_OBJECT_DISTRO = "distro";
   public static final String OUTPUT_OBJECT_NETWORK = "network";
   public static final String OUTPUT_OBJECT_APPMANAGER = "appmanager";
   public static final String OUTPUT_OBJECT_NODE_GROUP = "node group";
   public static final String OUTPUT_OBJECT_NODE = "node";
   public static final String OUTPUT_OBJECT_RESOURCEPOOL = "resourcepool";
   public static final String OUTPUT_OBJECT_TOPOLOGY = "topology";
   public static final String OUTPUT_OBJECT_TEMPLATE = "template";
   public static final String OUTPUT_OP_CREATE = "create";
   public static final String OUTPUT_OP_UPLOAD = "upload";
   public static final String OUTPUT_OP_CONFIG = "config";
   public static final String OUTPUT_OP_MODIFY = "modify";
   public static final String OUTPUT_OP_UPDATE = "update";
   public static final String OUTPUT_OP_EXPORT = "export";
   public static final String OUTPUT_OP_ADD = "add";
   public static final String OUTPUT_OP_EXPAND = "expand";
   public static final String OUTPUT_OP_LIST = "list";
   public static final String OUTPUT_OP_TARGET = "target";
   public static final String OUTPUT_OP_DELETE = "delete";
   public static final String OUTPUT_OP_START = "start";
   public static final String OUTPUT_OP_STOP = "stop";
   public static final String OUTPUT_OP_RESIZE = "resize";
   public static final String OUTPUT_OP_SET_PARAM = "setParam";
   public static final String OUTPUT_OP_RESET_PARAM = "resetParam";
   public static final String OUTPUT_OP_QUOTA_SET = "quota";
   public static final String OUTPUT_OP_FIX = "fix";
   public static final String OUTPUT_OP_RESULT_ADJUST = "adjusted";
   public static final String OUTPUT_OP_RESUME = "resume";
   public static final String OUTPUT_OP_UPGRADE = "upgrade";
   public static final String OUTPUT_OP_RESULT_FAIL = "failed";
   public static final String OUTPUT_OP_RESULT_CREAT = "created";
   public static final String OUTPUT_OP_RESULT_CONFIG = "configured";
   public static final String OUTPUT_OP_RESULT_MODIFY = "modified";
   public static final String OUTPUT_OP_RESULT_UPDATE = "updated";
   public static final String OUTPUT_OP_RESULT_DELETE = "deleted";
   public static final String OUTPUT_OP_RESULT_START = "started";
   public static final String OUTPUT_OP_RESULT_STOP = "stopped";
   public static final String OUTPUT_OP_RESULT_RESIZE = "resized";
   public static final String OUTPUT_OP_RESULT_RESUME = "resumed";
   public static final String OUTPUT_OP_RESULT_ADD = "added";
   public static final String OUTPUT_OP_RESULT_EXPAND = "expanded";
   public static final String OUTPUT_OP_RESULT_GET = "got";
   public static final String OUTPUT_OP_RESULT_UPLOAD = "uploaded";
   public static final String OUTPUT_OP_RESULT_RESET = "reset";
   public static final String OUTPUT_OP_RESULT_FIX = "fixed";
   public static final String OUTPUT_OP_RESULT_UPGRADE = "upgraded";
   public static final String OUTPUT_OP_NODEGROUP_MISSING = "must specify nodegroup name when node name is specified";
   public static final String OUTPUT_UNKNOWN_HOST = "unknown host";
   public static final String OUTPUT_HOST_TIMEOUT = "connection timeout";
   public static final String OUTPUT_LOG_INFO = "You can find task failure details in serengeti server logs: ";
   public static final String COMMON_LOG_FILE_PATH = "/opt/serengeti/logs/serengeti.log /opt/serengeti/logs/ironfan.log";
   public static final String HTTPS_CONNECTION_PREFIX = "https://";
   public static final String HTTPS_CONNECTION_SUFFIX = "/serengeti/api/";
   public static final String HTTPS_CONNECTION_API = "api/";
   public static final String HTTPS_CONNECTION_LOGIN_SUFFIX = "/serengeti/";
   public static final String PROPERTY_FILE = "cli.properties";
   public static final String CLI_HISTORY_FILE = "cli.history";

   public static final String INPUT_SPEC_PARAM = "\"spec\"";
   public static final String INPUT_RPNAMES_PARAM = "\"rpNames\"";
   public static final String INPUT_DSNAMES_PARAM = "\"dsNames\"";
   public static final String MULTI_INPUTS_CHECK = " cannot be empty.";
   public static final String INPUT_INSTANCE_NUM_PARAM = "\"instanceNum\"";
   public static final String INTEGER_INPUTS_CHECK = " should be larger than existed.";
   public static final String INVALID_VALUE = "Invalid value";
   public static final String INPUT_TOPOLOGY_INVALID_VALUE = "invalid topology value. The valid value is HVE or RACK_AS_RACK or HOST_AS RACK";
   public static final String TOPLOGY_FORMAT = "Accepted topology file format: for each line, rackname1: hostname1, hostname2,...";
   public static final String OVERWRITE_TOPOLOGY = "WARNING: you will overwrite the existing topology. Existing Hadoop clusters may be impacted and cannot work well if you remove some hosts these clusters are using.";
   public static final String TYPE_SPECFILE_CONFLICT = "--type will not take effect when you specify --specFile.";
   public static final String ZOOKEEPER_NOT_RESIZE = "zookeeper node groups cannot be resized.";
   public static final String PARAMS_EXCLUSION = " cannot be set at the same time.";
   public static final String PARAMS_EXCLUSION_PAIR_NETWORK_ADD_IP_DHCP = "ip and dhcp";
   public static final String PARAMS_EXCLUSION_PAIR_NETWORK_ADD_STATIC_DDNS = "static ip and dynamic DNS";
   public static final String PARAMS_NETWORK_ADD_IP_DHCP_NOT_NULL = "You must enter one of ip,dhcp.";
   public static final String PARAMS_NETWORK_ADD_DNS = "dns";
   public static final String PARAMS_NETWORK_ADD_GATEWAY = "gateway";
   public static final String PARAMS_NETWORK_ADD_MASK = "mask";
   public static final String PARAMS_NETWORK_ADD_OR = " or ";
   public static final String PARAMS_NETWORK_ADD_FORMAT_ERROR =
         " valid format is ips or ip ranges, eg. 255.0.0.1-100, 255.0.0.1 or 255.0.3.1-200,255.0.0.1-255";
   public static final String PARAMS_NETWORK_ADD_IP_ERROR = " you should provide valid ip or mask format.";
   public static final String PARAM_NOT_SUPPORTED = " input cannot be accepted, we only support: ";
   public static final String ROLE_HADOOP_NAME_NODE = "hadoop_namenode";
   public static final String ROLE_HADOOP_JOB_TRACKER = "hadoop_jobtracker";
   public static final String ROLE_HADOOP_DATANODE = "hadoop_datanode";
   public static final String ROLE_HADOOP_TASKTRACKER = "hadoop_tasktracker";
   public static final String ROLE_HADOOP_CLIENT = "hadoop_client";
   public static final String ROLE_HIVE = "hive";
   public static final String ROLE_HIVE_SERVER = "hive_server";
   public static final String ROLE_PIG = "pig";
   public static final String PARAM_CLUSTER_FAILED_INSTANCE_NUM_NEGATIVE = "instanceNum cannot be negative";
   public static final String PARAM_CLUSTER = "cluster name";
   public static final String PARAM_DISTRO = "distro name";
   public static final String PARAM_NETWORK_NAME = "network name";
   public static final String PARAM_NODEGROUP = "nodegroup name";
   public static final String PARAM_NOT_SPECIFIED = " is not specified.";
   public static final String PARAM_CANNOT_FIND_NETWORK = "Cannot find the specified network.";
   public static final String PARAM_NO_DEFAULT_DISTRO = "no default distro. Install a distribution and/or specify the distribution with the --distro parameter.";
   public static final String PARAM_NOT_CONTAIN_HORIZONTAL_LINE = " cannot contain '-'.";
   public static final String PARAM_NOT_CONTAIN_BLANK_SPACE = " cannot contain blank space.";
   public static final String PARAM_NOT_CONTAIN_SPECFILE = "You should specify --specFile .";
   public static final String PARAM_SHOULD_SPECIFY_DISK = "You should specify --disk .";
   public static final String PARAM_SHOULD_HAVE_COMPUTE_ONLY_GROUP = "If the cluster is MRv1, then it must have compute only node group(s), and set/resetParam is only applicable to compute only node groups. On the other hand, we do not support elasticity on MRv2 (YARN) clusters yet.";
   public static final String PARAM_SHOULD_BE_LESS_THAN_DEPLOYED = "should be less than or equal to deployed compute only nodes:";
   public static final String PARAM_MIN_CAN_NOT_BE_LARGER_THAN_MAX = "minComputeNodeNum can not be larger than maxComputeNodeNum.";
   public static final String PARAM_SHOULD_SPECIFY_RP_DS = "You should specify --rpNames or --dsNames";
   public static final int FORMAT_COLUMN_DISTANCE = 2;
   public static final String FORMAT_TABLE_COLUMN_NAME = "NAME";
   public static final String FORMAT_TABLE_COLUMN_GROUP_NAME = "GROUP NAME";
   public static final String FORMAT_TABLE_COLUMN_NODE_NAME = "NODE NAME";
   public static final String FORMAT_TABLE_COLUMN_NODE_VERSION = "AGENT VERSION";
   public static final String FORMAT_TABLE_COLUMN_STATUS = "STATUS";
   public static final String FORMAT_TABLE_COLUMN_IP = "IP";
   public static final String FORMAT_TABLE_COLUMN_HDFS_IP = "HDFS IP";
   public static final String FORMAT_TABLE_COLUMN_MAPRED_IP = "MAPRED IP";
   public static final String FORMAT_TABLE_COLUMN_VENDOR = "VENDOR";
   public static final String FORMAT_TABLE_COLUMN_VERSION = "VERSION";
   public static final String FORMAT_TABLE_COLUMN_HVE = "HVE";
   public static final String FORMAT_TABLE_COLUMN_ROLES = "ROLES";
   public static final String FORMAT_TABLE_COLUMN_INSTANCE = "INSTANCE";
   public static final String FORMAT_TABLE_COLUMN_CPU = "CPU";
   public static final String FORMAT_TABLE_COLUMN_MEM = "MEM(MB)";
   public static final String FORMAT_TABLE_COLUMN_IOSHARE = "IO SHARES";
   public static final String FORMAT_TABLE_COLUMN_TARGET_NUM = "TARGET NUM";
   public static final String FORMAT_TABLE_COLUMN_TYPE = "TYPE";
   public static final String FORMAT_TABLE_COLUMN_SIZE = "SIZE(GB)";
   public static final String FORMAT_TABLE_COLUMN_RUNNING_NODES = "RUNNING NODE NUMBER";
   public static final String FORMAT_TABLE_COLUMN_IOSHARES = "I/O PRIORITY";
   public static final String FORMAT_TABLE_COLUMN_RACK = "RACK";
   public static final String FORMAT_TABLE_COLUMN_HOST = "HOST";
   public static final String FORMAT_TABLE_COLUMN_PHYSICAL_HOST = "PHYSICAL HOST";
   public static final String FORMAT_TABLE_COLUMN_SPACE = "SPACE(GB)";
   public static final String FORMAT_TABLE_COLUMN_FREE_SPACE = "FREE_SPACE(GB)";
   public static final String FORMAT_TABLE_COLUMN_PORT_GROUP = "PORTGROUP";
   public static final String FORMAT_TABLE_COLUMN_IP_RANGES = "IP_RANGES";
   public static final String FORMAT_TABLE_COLUMN_FREE_IPS = "FREE_IPS";
   public static final String FORMAT_TABLE_COLUMN_ASSIGNED_IPS = "ASSIGNED_IPS";
   public static final String FORMAT_TABLE_COLUMN_DNS1 = "DNS1";
   public static final String FORMAT_TABLE_COLUMN_DNS2 = "DNS2";
   public static final String FORMAT_TABLE_COLUMN_GATEWAY = "GATEWAY";
   public static final String FORMAT_TABLE_COLUMN_MASK = "MASK";
   public static final String FORMAT_TABLE_COLUMN_DNS_TYPE = "DNS_TYPE";
   public static final String FORMAT_TABLE_COLUMN_GENERATE_HOSTNAME = "GENERATE_HOSTNAME";
   public static final String FORMAT_TABLE_COLUMN_NODE = "NODE";
   public static final String FORMAT_TABLE_COLUMN_NODE_GROUP_NAME = "NODEGROUP";
   public static final String FORMAT_TABLE_COLUMN_CLUSTER_NAME = "CLUSTER";
   public static final String FORMAT_TABLE_COLUMN_PATH = "PATH";
   public static final String FORMAT_TABLE_COLUMN_RAM_MB = "RAM(MB)";
   public static final String FORMAT_TABLE_COLUMN_CPU_MHZ = "CPU(MHZ)";
   public static final String FORMAT_TABLE_COLUMN_TASK = "TASK";
   public static final String FORMAT_TABLE_COLUMN_HOSTS = "HOSTS";
   public static final String FORMAT_TABLE_COLUMN_REGEX = "REG EX";
   public static final String FORMAT_TABLE_COLUMN_ERROR = "Error Message";
   public static final String FORMAT_TABLE_COLUMN_DESCRIPTION = "DESCRIPTION";
   public static final String FORMAT_TABLE_COLUMN_URL = "URL";
   public static final String FORMAT_TABLE_COLUMN_USERNAME = "USERNAME";
   public static final String FORMAT_TABLE_COLUMN_VM_MOID = "MOID";
   public static final String FORMAT_TABLE_COLUMN_VM_LAST_MODIFIED = "LAST MODIFIED";
   public static final String FORMAT_TABLE_COLUMN_VM_TAG = "TAG";
   public static final String DEFAULT_DISTRO = "bigtop";
   public static final String PARAM_PROMPT_CONTINUE_MESSAGE = "Are you sure you want to continue (Y/N)? ";
   public static final String PARAM_PROMPT_ADD_CERTIFICATE_MESSAGE = "Are you sure you want to add this certificate (Y/N)? ";
   public static final String PARAM_PROMPT_SSL_CERTIFICATE_MESSAGE = "Enter the file path of the ssl certificate: ";
   public static final String PARAM_CLUSTER_IN_BLACK_LIST_WARNING =
         "not allowed to customize in Serengeti.";
   public static final String PARAM_CLUSTER_NOT_TAKE_EFFECT = "Your settings will not take effect.";
   public static final String PARAM_CLUSTER_NOT_IN_WHITE_LIST_WARNING =
         "not regular configuration properties. ";
   public static final String PARAM_CLUSTER_CONFIG_RUNNING_JOB_WARNING =
         "Applying new Hadoop configuration will restart Hadoop processes and may lead to termination of running jobs.";
   public static final String PARAM_PROMPT_UPGRADE_CLUSTER_WARNING =
         "cluster upgrade may need to restart the services running in the cluster nodes. You should stop all running jobs before running cluster upgrade.";
   public static final String PARAM_CLUSTER_SPEC_HA_ERROR = "the ha property must be 'off', 'on' or 'ft' in specification file ";
   public static final String CONNECT_UNAUTHORIZATION_CONNECT =
         "The connection is refused, may be invalid username or invalid password. Try to reconnect.";
   public static final String CONNECT_UNAUTHORIZATION_OPT =
         "The connection is refused, may be overtime session. Try to reconnect.";
   public static final String CONNECT_USER_NAME = "The user name";
   public static final String CONNECT_PASSWORD = "The password";
   public static final String CONNECT_CAN_NOT_BE_NULL = " cannot be null.";
   public static final String CONNECT_PORT_IS_WRONG = "The port is wrong.";
   public static final String CONNECT_ENTER_USER_NAME = "Enter the username: ";
   public static final String CONNECT_ENTER_PASSWORD = "Enter the password: ";
   public static final String CONNECT_CHECK_LOGIN = "You are not logged in. Run the connect command to log in.";
   public static final String CONNECT_SESSION_TIME_OUT = "The session timed out. Run the connect command to restart the session.";
   public static final String OUTPUT_OBJECT_CONNECT = "connect";
   public static final String NODE_RUNNING_STATUS = "Service Ready";
   public static final String AT_LEAST_ONE_OPTION = "You must use at least one option.";
   public static final String ENTER_PASSWORD = "Enter the password: ";
   public static final String CONFIRM_PASSWORD = "Confirm the password: ";
   public static final String PASSWORD_CONFIRMATION_FAILED = "Password confirmation failed. The passwords must match.";
   public static final String RESUME_DONOT_NEED_SET_PASSWORD = "Run the 'cluster create --resume' command, but do not include the '--password' parameter.";
   public static final String FAILED_NODES_MESSAGE = "The failed nodes: ";
   public static final String IRONFAN = "Default";
   public static final String CANNOT_SPECIFY_TWO_PATH_FOR_EXPORT = "You cannot specify 2 paths for spec file.";
   public static final String CANNOT_EXPORT_CLUSTER_AND_RACK_OPOLOGY_SIMULTANEOUSLY = "You cannot specify export cluster and rack topology simultaneously.";

   public static final String CLUSTER_NOT_EXIST = "Cluster doesn't exist";
   public static final String EXPORT_TYPE_SPEC = "spec";
   public static final String EXPORT_TYPE_RACK = "rack";
   public static final String EXPORT_TYPE_IP = "ip";
   public static final String EXPORT_TYPE_IP_2_FQDN = "ip2fqdn";
   public static final String UNKNOWN_EXPORT_TYPE = "export type is unknown";

   public static final String LOGIN_USERNAME = "username";
   public static final String LOGIN_PASSWORD = "password";
   public static final String OUTPUT_OP_RESULT_SUCCEEDED = "succeeded";
   public static final String OUTPUT_OP_RESULT_WARN = "warning";
   public static final String OUTPUT_OBJECT_MGMTVMCFG = "Management VM configuration";
   public static final String OUTPUT_OP_GET = "get";

   public static final String WARNING_INSTANT_CLONE_WITH_HA = "Warning: you are using instant clone with HA function "
         + "for node group %s. Currently the instant clone does not support HA function well, so the cluster creation "
         + "might fail or the HA function might not work.";
   public static final String REST_PARAM_SKIP_REFRESH_VC = "skiprefreshvc";

   enum YES_NO {
      yes, no
   }
   public static final String FORCE_CLUSTER_OPERATION_KEY = "force";

   public static final String REST_PATH_CLUSTER_RECOVER = "recover";
   public static final String OUTPUT_OBJECT_CLUSTER_ALL = "All clusters";
   public static final String OUTPUT_OP_RESULT_RECOVER_SUCC = "are recovered";
   public static final String OUTPUT_OP_RESULT_RECOVER = "recover";
}
