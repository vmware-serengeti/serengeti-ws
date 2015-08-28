/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.util.collection;

public class CollectionConstants {
    public final static String SENSITIVE_DATA_FILE = "/sensitive-data.json";

    public static final String COLLECT_DATA_JOB_NAME = "collectDataJob";

    public static final String DEFAULT_SWITCH_NAME = "dataCollector.default.switchName";
    public static final String PHONE_HOME_SWITCH_NAME = "serengeti.ph.enable";
    public final static String BDE_FOOTPRINT = "bde.footprint";
    public final static String BDE_ENVIRONMENTAL_INFORMATION = "bde.environmental_information";
    public final static String BDE_PRODUCT_INSTANCE = "bde.product_instance";
    public final static String BDE_OPERATION = "bde.operation";
    public final static String BDE_CLUSTER_SNAPSHOT = "bde.cluster_snapshot";

    public final static String OBJECT_ID = "id";
    public final static String FOOTPRINT_VERSION = "version";
    public final static String FOOTPRINT_DEPLOY_TIME = "deploy_time";
    public final static String FOOTPRINT_VC_CPU_QUOTA_SIZE = "vc_cpu_quota_size";
    public final static String FOOTPRINT_VC_MEM_QUOTA_SIZE = "vc_mem_quota_size";
    public final static String FOOTPRINT_RESOURCEPOOL_CPU_QUOTA_SIZE = "resourcepool_cpu_quota_size";
    public final static String FOOTPRINT_RESOURCEPOOL_MEM_QUOTA_SIZE = "resourcepool_mem_quota_size";
    public final static String FOOTPRINT_DATASTORE_QUOTA_SIZE = "datastore_quota_size";
    public final static String FOOTPRINT_IS_INIT_RESOURCE = "is_init_resource";
    public final static String FOOTPRINT_HOST_NUM_OF_VC = "host_num_of_vc";
    public final static String FOOTPRINT_HOST_NUM_OF_RESOURCE_POOLS = "host_num_of_resource_pools";
    public final static String FOOTPRINT_HOST_NUM_OF_CLUSTERS = "host_num_of_clusters";
    public final static String FOOTPRINT_NUM_OF_HADOOP_CLUSTERS = "num_of_hadoop_clusters";
    public final static String FOOTPRINT_NUM_OF_HADOOP_NODES = "num_of_hadoop_nodes";

    public final static String ENVIRONMENTAL_INFO_VERSION_OF_VCENTER = "version_of_vCenter";
    public final static String ENVIRONMENTAL_INFO_VERSION_OF_ESXI = "version_of_ESXi";
    public final static String ENVIRONMENTAL_INFO_TYPE_OF_STORAGE = "type_of_storage";
    public final static String ENVIRONMENTAL_INFO_DISTROS_OF_HADOOP = "distros_of_hadoop";
    public final static String ENVIRONMENTAL_INFO_APP_MANAGERS = "app_managers";

    public final static String PRODUCT_INSTANCE_NAME = "name";
    public final static String PRODUCT_INSTANCE_VERSION = "version";
    public final static String PRODUCT_INSTANCE_EDITION = "edition";
    public final static String PRODUCT_INSTANCE_NAME_VALUE = "vSphere Big Data Extensions";
    public final static String PRODUCT_INSTANCE_EDITION_VALUE = "Enterprise";

    public final static String METHOD_CREATE_CLUSTER = "createCluster";
    public final static String METHOD_CONFIG_CLUSTER = "configCluster";
    public final static String METHOD_RESIZE_CLUSTER = "resizeCluster";
    public final static String METHOD_SCALE_NODE_GROUP_RESOURCE = "scaleNodeGroupResource";

    public final static String OPERATION_NAME = "operation_name";
    public final static String OPERATION_BEGIN_TIME = "begin_time";
    public final static String OPERATION_PARAMETERS = "operation_parameters";
    public final static String OPERATION_END_TIME = "end_time";
    public final static String OPERATION_STATUS = "operation_status";
    public final static String TASK_ID = "task_id";
    public final static String CLUSTER_ID = "cluster_id";
    public final static String ASYNCHRONIZATION_PREFIX = "asynchronization_";
    public final static String SYNCHRONIZATION_PREFIX = "synchronization_";

    public final static String CLUSTER_SNAPSHOT_USE_EXTERNAL_HDFS = "use_external_hdfs";
    public final static String CLUSTER_SNAPSHOT_HADOOP_ECOSYSTEM_INFORMATION = "hadoop_ecosystem_information";
    public final static String CLUSTER_SNAPSHOT_DISTRO = "distro";
    public final static String CLUSTER_SNAPSHOT_DISTRO_VERSION = "distro_version";
    public final static String CLUSTER_SNAPSHOT_DISTRO_VENDOR = "distro_vendor";
    public final static String CLUSTER_SNAPSHOT_TYPE_OF_NETWORK = "type_of_network";
    public final static String CLUSTER_SNAPSHOT_NODE_NUMBER = "node_number";
    public final static String CLUSTER_SNAPSHOT_CPU_NUMBER = "cpu_number";
    public final static String CLUSTER_SNAPSHOT_MEMORY_SIZE = "memory_size";
    public final static String CLUSTER_SNAPSHOT_DATASTORE_SIZE = "datastore_size";
    public final static String CLUSTER_SNAPSHOT_CLUSTER_SPEC = "cluster_spec";
    public final static String STATIC_IP = "STATIC IP";
    public final static String DHCP = "DHCP";

    public final static String PIG = "Pig";
    public final static String HIVE = "Hive";
    public final static String OOZIE = "Oozie";
    public final static String GANGLIA = "Ganglia";
    public final static String NAGIOS = "Nagios";
    public final static String STORM = "Storm";
    public final static String IMPALA = "Impala";
    public final static String SQOOP = "Sqoop";
    public final static String SPARK = "Spark";
    public final static String SOLR = "Solr";

    public final static String VC_UUID = "vc_uuid";
    public final static String VC_VERSION = "vc_version";
    public final static String VC_RELATION = "vc_relation";
}

