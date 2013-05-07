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
package com.vmware.bdd.service.job;

public interface JobConstants {
   public static final String CREATE_CLUSTER_JOB_NAME = "createClusterJob";
   public static final String DELETE_CLUSTER_JOB_NAME = "deleteClusterJob";
   public static final String START_CLUSTER_JOB_NAME = "startClusterJob";
   public static final String RESIZE_CLUSTER_JOB_NAME = "resizeClusterJob";
   public static final String RESUME_CLUSTER_JOB_NAME = "resumeClusterJob";
   public static final String STOP_CLUSTER_JOB_NAME = "stopClusterJob";
   public static final String CONFIG_CLUSTER_JOB_NAME = "configClusterJob";
   public static final String SET_MANUAL_ELASTICITY_JOB_NAME = "setManualElasticityJob";
   public static final String QUERY_CLUSTER_JOB_NAME = "queryClusterJob";
   
   public static final String CLUSTER_SPEC_JOB_PARAM = "clusterSpec";
   public static final String CLUSTER_ADDED_NODES_JOB_PARAM = "clusterAddedNodes";
   public static final String CLUSTER_EXISTING_NODES_JOB_PARAM = "clusterExistingNodes";
   public static final String CLUSTER_DELETED_NODES_JOB_PARAM = "clusterDeletedNodes";
   public static final String CLUSTER_ID_JOB_PARAM = "clusterId";
   public static final String CLUSTER_NAME_JOB_PARAM = "clusterName";
   public static final String TARGET_NAME_JOB_PARAM = "targetName";
   public static final String CLUSTER_USED_IP_JOB_PARAM = "clusterUsedIps";
   public static final String GROUP_NAME_JOB_PARAM = "groupName";
   public static final String GROUP_INSTANCE_NEW_NUMBER_JOB_PARAM = "newInstanceNumber";
   public static final String GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM = "oldInstanceNumber";
   public static final String GROUP_ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM = "activeComputeNodeNumber";
   public static final String HADOOP_JOBTRACKER_IP_JOB_PARAM = "hadoopJobTrackerIP";
   public static final String TIMESTAMP_JOB_PARAM = "timeStamp";
   public static final String CLUSTER_OPERATION_SUCCESS = "cluster.isSuccess";
   public static final String NODE_OPERATION_SUCCESS = "node.isSuccess";
   public static final String CLUSTER_DELETE_VM_OPERATION_SUCCESS = "cluster.isDeleted";
   public static final String CLUSTER_CREATE_VM_OPERATION_SUCCESS = "cluster.isCreated";
   public static final String CLUSTER_SET_AUTO_ELASTICITY_SUCCESS = "cluster.isSet";
   public static final String CLUSTER_OPERATION_INIT_STATUS = "clusterOperationInitStatus";
   public static final String CLUSTER_SUCCESS_STATUS_JOB_PARAM = "clusterSuccessStatus";
   public static final String CLUSTER_FAILURE_STATUS_JOB_PARAM = "clusterFailureStatus";
   public static final String VERIFY_NODE_STATUS_SCOPE_PARAM = "verifyNodeScope";
   public static final String GROUP_NODE_SCOPE_VALUE = "group";
   public static final String CLUSTER_NODE_SCOPE_VALUE = "cluster";
   public static final String CURRENT_COMMAND_WORK_DIR = "currentCommandWorkDir";
   public static final String CURRENT_ERROR_MESSAGE= "currentErrorMessage";
   public static final String CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM = "ResourceReservationId";
   public static final String EXPECTED_NODE_STATUS = "expectedNodeStatus";
   public static final String SUB_JOB_ENABLED = "sub.job.enabled";
   public static final String SUB_JOB_NUMBER = "sub.job.number";
   public static final String SUB_JOB_PARAMETERS_NUMBER = "sub.job.parameters.number.";
   public static final String SUB_JOB_PARAMETERS_KEY_PREFIX = "sub.job.parameters.parameter.key.";
   public static final String SUB_JOB_PARAMETERS_VALUE_PREFIX = "sub.job.parameters.parameter.value.";
   public static final String SUB_JOB_NODE_NAME = "sub.job.node.name";
   public static final String SUB_JOB_NODES_SUCCEED = "sub.job.nodes.suceed";
   public static final String SUB_JOB_NODES_FAIL = "sub.job.nodes.fail";
   
   public static final String NODE_SCALE_JOB_NAME = "scaleSingleNodeJob";
   public static final String NODE_SCALE_CPU_NUMBER = "node.scale.cpu";
   public static final String NODE_SCALE_MEMORY_SIZE = "node.scale.memory";
}
