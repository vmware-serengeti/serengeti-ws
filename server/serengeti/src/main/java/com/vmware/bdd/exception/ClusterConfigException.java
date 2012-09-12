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
package com.vmware.bdd.exception;

import java.util.List;

import com.vmware.bdd.apitypes.TopologyType;

public class ClusterConfigException extends BddException {
   private static final long serialVersionUID = 1L;
   public ClusterConfigException() {
   }

   public ClusterConfigException(Throwable cause, String errorId, Object... detail) {
      super(cause, "CLUSTER_CONFIG", errorId, detail);
   }

   public static ClusterConfigException CLUSTER_NAME_MISSING() {
      return new ClusterConfigException(null, "CLUSTER_NAME_MISSING");
   }

   public static ClusterConfigException CLUSTER_CONFIG_NOT_FOUND(String clusterName) {
      return new ClusterConfigException(null, "CLUSTER_CONFIG_NOT_FOUND", clusterName);
   }
   public static ClusterConfigException UNSUPPORTED_HADOOP_ROLE(String roleName, String distro) {
      return new ClusterConfigException(null, "UNSUPPORTED_HADOOP_ROLE", roleName, distro);
   }
   public static ClusterConfigException MISSING_HADOOP_ROLE(String roleName, String distro) {
      return new ClusterConfigException(null, "MISSING_HADOOP_ROLE", roleName, distro);
   }
   public static ClusterConfigException NO_HADOOP_ROLE_SPECIFIED(String group) {
      return new ClusterConfigException(null, "NO_HADOOP_ROLE_SPECIFIED", group);
   }
   public static ClusterConfigException MORE_THAN_ONE_MASTER_NODE(String clusterName) {
      return new ClusterConfigException(null, "MORE_THAN_ONE_MASTER_NODE", clusterName);
   }
   public static ClusterConfigException NETWORK_IS_NOT_SPECIFIED(int size, String clusterName) {
      return new ClusterConfigException(null, "NETWORK_IS_NOT_SPECIFIED", size, clusterName);
   }
   public static ClusterConfigException NETWORK_IS_NOT_FOUND(String networkName, String clusterName) {
      return new ClusterConfigException(null, "NETWORK_IS_NOT_FOUND", networkName, clusterName);
   }
   public static ClusterConfigException NO_RESOURCE_POOL_ADDED() {
      return new ClusterConfigException(null, "NO_RESOURCE_POOL_ADDED");
   }
   public static ClusterConfigException NO_DATASTORE_ADDED() {
      return new ClusterConfigException(null, "NO_DATASTORE_ADDED");
   }
   public static ClusterConfigException NO_SHARED_DATASTORE() {
      return new ClusterConfigException(null, "NO_SHARED_DATASTORE");
   }
   public static ClusterConfigException INVALID_INSTANCE_NUMBER(int num, String clusterName, String groupName) {
      return new ClusterConfigException(null, "INVALID_INSTANCE_NUMBER", num, clusterName, groupName);
   }
   public static ClusterConfigException MORE_THAN_ONE_NAMENODE_GROUP(String clusterName) {
      return new ClusterConfigException(null, "MORE_THAN_ONE_NAMENODE_GROUP", clusterName);
   }
   public static ClusterConfigException MORE_THAN_ONE_JOBTRACKER_GROUP(String clusterName) {
      return new ClusterConfigException(null, "MORE_THAN_ONE_JOBTRACKER_GROUP", clusterName);
   }
   public static ClusterConfigException INVALID_APP_CONFIG_VALUE(List<String> configNames) {
      return new ClusterConfigException(null, "INVALID_APP_CONFIG_VALUE", configNames);
   }
   public static ClusterConfigException INVALID_PLACEMENT_POLICIES(List<String> errors) {
      return new ClusterConfigException(null, "INVALID_PLACEMENT_POLICIES", errors);
   }
   public static ClusterConfigException LACK_PHYSICAL_HOSTS() {
      return new ClusterConfigException(null, "LACK_PHYSICAL_HOSTS");
   }
   public static ClusterConfigException INVALID_ROLES(List<String> errors) {
      return new ClusterConfigException(null, "INVALID_ROLES", errors);
   }
   public static ClusterConfigException INVALID_TOPOLOGY_POLICY(TopologyType topology, String reason) {
      return new ClusterConfigException(null, "INVALID_TOPOLOGY_POLICY", topology, reason);
   }
}
