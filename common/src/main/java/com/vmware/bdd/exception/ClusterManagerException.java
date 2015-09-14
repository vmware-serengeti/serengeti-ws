/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

public class ClusterManagerException extends BddException {
   private static final long serialVersionUID = 1l;

   public ClusterManagerException() {
   }

   public ClusterManagerException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "CLUSTER", errorId, detail);
   }

   public static ClusterManagerException MANIFEST_NOT_FOUND_ERROR(
         String clusterName) {
      return new ClusterManagerException(null, "MANIFEST_NOT_FOUND_ERROR",
            clusterName);
   }

   public static ClusterManagerException DELETION_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_DELETE", clusterName,
            reason);
   }

   public static ClusterManagerException UPGRADE_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_UPGRADE", clusterName, reason);
   }

   public static ClusterManagerException START_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_START", clusterName,
            reason);
   }

   public static ClusterManagerException ALREADY_STARTED_ERROR(
         String clusterName) {
      return new ClusterManagerException(null, "IS_RUNNING", clusterName);
   }

   public static ClusterManagerException STOP_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_STOP", clusterName,
            reason);
   }

   public static ClusterManagerException ALREADY_STOPPED_ERROR(
         String clusterName) {
      return new ClusterManagerException(null, "IS_STOPPED", clusterName);
   }

   public static ClusterManagerException UPDATE_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_UPDATE", clusterName,
            reason);
   }

   public static ClusterManagerException NODEGROUP_NOT_FOUND_ERROR(
         String nodeGroupName) {
      return new ClusterManagerException(null, "NODEGROUP_NOT_FOUND",
            nodeGroupName);
   }

   public static ClusterManagerException ROLES_NOT_SUPPORTED(List<String> roles) {
      return new ClusterManagerException(null, "ROLES_NOT_SUPPORTED", roles);
   }

   public static ClusterManagerException SET_MANUAL_ELASTICITY_NOT_ALLOWED_ERROR(String reason) {
      return new ClusterManagerException(null, "CANNOT_SET_MANUAL_ELASTICITY", reason);
   }

   public static ClusterManagerException SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_SET_AUTO_ELASTICITY",
            clusterName, reason);
   }

   public static ClusterManagerException FAILED_TO_SET_AUTO_ELASTICITY_ERROR(
		String clusterName, String reason) {
	  return new ClusterManagerException(null,
			"FAILED_TO_SET_AUTO_ELASTICITY", clusterName, reason);
   }

   public static ClusterManagerException PRIORITIZE_CLUSTER_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_PRIORITIZE_CLUSTER",
            clusterName, reason);
   }

   public static ClusterManagerException PRIORITIZE_CLUSTER_FAILED(
         String clusterName, int count, int expected) {
      return new ClusterManagerException(null, "PRIORITIZE_CLUSTER_FAILED",
            clusterName, expected, count);
   }

   public static ClusterManagerException ALREADY_LATEST_VERSION_ERROR(String clusterName) {
      return new ClusterManagerException(null, "IS_THE_LATEST_VERSION", clusterName);
   }

   public static ClusterManagerException OLD_VERSION_ERROR(String clusterName) {
      return new ClusterManagerException(null, "OLD_VERSION", clusterName);
   }

   public static ClusterManagerException NO_NEED_TO_RESIZE(String clusterName, String nodeGroupName, int instanceNum) {
      return new ClusterManagerException(null, "NO_NEED_TO_RESIZE", clusterName, nodeGroupName, instanceNum);
   }

   public static ClusterManagerException NODE_GROUP_HAS_EXISTED(String clusterName, String nodeGroupName) {
      return new ClusterManagerException(null, "NODE_GROUP_HAS_EXISTED", clusterName, nodeGroupName);
   }

    public static ClusterManagerException NODE_GROUP_CANNOT_BE_ZERO(String clusterName) {
        return new ClusterManagerException(null, "NODE_GROUP_CANNOT_BE_ZERO", clusterName);
    }

    public static ClusterManagerException ADD_NODE_GROUP_FAILED(String clusterName) {
        return new ClusterManagerException(null, "ADD_NODE_GROUP_FAILED", clusterName);
    }

//   public static ClusterManagerException NODE_GROUP_CANNOT_BE_ZERO(String clusterName, String nodeGroupName) {
//      return new ClusterManagerException(null, "NODE_GROUP_CANNOT_BE_ZERO", clusterName, nodeGroupName);
//   }

}
