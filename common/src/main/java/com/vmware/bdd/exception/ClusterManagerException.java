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

   public static ClusterManagerException SHRINK_OP_NOT_SUPPORTED(
         String nodeGroupName, int newInstanceNum, int definedInstanceNum) {
      return new ClusterManagerException(null, "SHRINK_OP_NOT_SUPPORTED",
            nodeGroupName, definedInstanceNum, newInstanceNum);
   }

   public static ClusterManagerException ROLES_NOT_SUPPORTED(List<String> roles) {
      return new ClusterManagerException(null, "ROLES_NOT_SUPPORTED", roles);
   }

   public static ClusterManagerException SET_MANUAL_ELASTICITY_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_SET_MANUAL_ELASTICITY",
            clusterName, reason);
   }

   public static ClusterManagerException SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
         String clusterName, String reason) {
      return new ClusterManagerException(null, "CANNOT_SET_AUTO_ELASTICITY",
            clusterName, reason);
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
}
