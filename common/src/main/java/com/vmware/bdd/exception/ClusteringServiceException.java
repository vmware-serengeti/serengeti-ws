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

public class ClusteringServiceException extends BddException {
   private static final long serialVersionUID = 1l;

   public ClusteringServiceException() {
   }

   public ClusteringServiceException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "CLUSTERING_SERVICE", errorId, detail);
   }

   public static ClusteringServiceException TEMPLATE_ID_NOT_FOUND() {
      return new ClusteringServiceException(null, "TEMPLATE_ID_NOT_FOUND");
   }

   public static ClusteringServiceException TEMPLATE_VM_NOT_FOUND() {
      return new ClusteringServiceException(null, "TEMPLATE_VM_NOT_FOUND");
   }

   public static ClusteringServiceException TARGET_VC_RP_NOT_FOUND(
         String vcClusterName, String vcRpName) {
      return new ClusteringServiceException(null, "TARGET_VC_RP_NOT_FOUND",
            vcRpName, vcClusterName);
   }

   public static ClusteringServiceException TARGET_VC_DATASTORE_NOT_FOUND(
         String dsName) {
      return new ClusteringServiceException(null,
            "TARGET_VC_DATASTORE_NOT_FOUND", dsName);
   }

   public static ClusteringServiceException GET_TEMPLATE_NETWORK_ERROR(
         String vmName) {
      return new ClusteringServiceException(null, "GET_TEMPLATE_NETWORK_ERROR",
            vmName);
   }

   public static ClusteringServiceException DELETE_CLUSTER_VM_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null, "DELETE_CLUSTER_VM_FAILED",
            clusterName);
   }

   public static ClusteringServiceException TEMPLATE_VM_NO_OS_DISK() {
      return new ClusteringServiceException(null, "TEMPLATE_VM_NO_OS_DISK");
   }

   public static ClusteringServiceException VM_CREATION_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null, "VM_CREATION_FAILED",
            clusterName);
   }

   public static ClusteringServiceException CREATE_FOLDER_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null, "CREATE_FOLDER_FAILED",
            clusterName);
   }

   public static ClusteringServiceException CREATE_RESOURCE_POOL_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null,
            "CREATE_RESOURCE_POOL_FAILED", clusterName);
   }

   public static ClusteringServiceException SET_AUTO_ELASTICITY_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null, "SET_AUTO_ELASTICITY_FAILED",
            clusterName);
   }

   public static ClusteringServiceException CLUSTER_OPERATION_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null, "CLUSTER_OPERATION_FAILED",
            clusterName);
   }

   public static ClusteringServiceException VM_IS_NOT_FOUND(String vmId) {
      return new ClusteringServiceException(null, "VM_IS_NOT_FOUND", vmId);
   }

   public static ClusteringServiceException DISABLE_HA_FAILED(Throwable cause,
         String vmName) {
      return new ClusteringServiceException(cause, "DISABLE_HA_FAILED", vmName);
   }

   public static ClusteringServiceException ENABLE_FT_FAILED(Throwable cause,
         String vmName) {
      return new ClusteringServiceException(cause, "ENABLE_FT_FAILED", vmName);
   }

   public static ClusteringServiceException CPU_NUMBER_MORE_THAN_ONE(
         String vmName) {
      return new ClusteringServiceException(null, "CPU_NUMBER_MORE_THAN_ONE",
            vmName);
   }

   public static ClusteringServiceException RECONFIGURE_IO_SHARE_FAILED(
         String clusterName) {
      return new ClusteringServiceException(null,
            "RECONFIGURE_IO_SHARE_FAILED", clusterName);
   }

   public static ClusteringServiceException CANNOT_GET_IP_ADDRESS(String vmName) {
      return new ClusteringServiceException(null, "CANNOT_GET_IP_ADDRESS",
            vmName);
   }

   public static ClusteringServiceException VM_NAME_VIOLATE_NAME_PATTERN(
         String vmName) {
      return new ClusteringServiceException(null,
            "VM_NAME_VIOLATE_NAME_PATTERN", vmName);
   }

   public static ClusteringServiceException VM_VIOLATE_PLACEMENT_POLICY(
         List<String> vmNames) {
      return new ClusteringServiceException(null,
            "VM_VIOLATE_PLACEMENT_POLICY", vmNames);
   }

   public static ClusteringServiceException VM_STATUS_ERROR(String vmName, String actual, String expected) {
      return new ClusteringServiceException(null, "VM_STATUS_ERROR", vmName, actual, expected);
   }
}
