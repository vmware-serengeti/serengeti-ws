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


public class ClusterHealServiceException extends BddException {
   private static final long serialVersionUID = 1l;

   public ClusterHealServiceException() {
   }

   public ClusterHealServiceException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "CLUSTER_HEAL_SERVICE", errorId, detail);
   }

   public static ClusterHealServiceException NOT_SUPPORTED(String clusterName,
         String errMsg) {
      return new ClusterHealServiceException(null, "NOT_SUPPORTED",
            clusterName, errMsg);
   }

   public static ClusterHealServiceException NOT_NEEDED(String clusterName) {
      return new ClusterHealServiceException(null, "NOT_NEEDED", clusterName);
   }

   public static ClusterHealServiceException NOT_ENOUGH_STORAGE(
         String nodeName, String errMsg) {
      return new ClusterHealServiceException(null, "NOT_ENOUGH_STORAGE",
            nodeName, errMsg);
   }

   public static ClusterHealServiceException FAILED_CREATE_REPLACEMENT_VM(
         String nodeName) {
      return new ClusterHealServiceException(null,
            "FAILED_CREATE_REPLACEMENT_VM", nodeName);
   }

   public static ClusterHealServiceException FAILED_POWER_OFF_VM(String vmName) {
      return new ClusterHealServiceException(null, "FAILED_POWER_OFF_VM",
            vmName);
   }

   public static ClusterHealServiceException FAILED_POWER_ON_VM(String vmName) {
      return new ClusterHealServiceException(null, "FAILED_POWER_ON_VM",
            vmName);
   }

   public static ClusterHealServiceException FAILED_DELETE_VM(String vmName) {
      return new ClusterHealServiceException(null, "FAILED_DELETE_VM", vmName);
   }

   public static ClusterHealServiceException FAILED_RENAME_VM(String vmName,
         String newName) {
      return new ClusterHealServiceException(null, "FAILED_RENAME_VM", vmName,
            newName);
   }

   public static ClusterHealServiceException FAILED_TO_GET_IP(String nodeName) {
      return new ClusterHealServiceException(null, "FAILED_TO_GET_IP", nodeName);
   }

   public static ClusterHealServiceException ERROR_STATUS(String nodeName, String errMsg) {
      return new ClusterHealServiceException(null, "ERROR_STATUS", nodeName, errMsg);
   }

   public static ClusterHealServiceException FAILED_TO_DETACH_VIRTUALDISK(String vmdkPath, String vmName) {
      return new ClusterHealServiceException(null, "FAILED_TO_DETACH_VIRTUALDISK", vmdkPath, vmName);
   }

   public static ClusterHealServiceException FAILED_TO_REPLACE_BAD_DATA_DISKS(String vmName) {
      return new ClusterHealServiceException(null, "FAILED_TO_REPLACE_BAD_DATA_DISKS", vmName);
   }

   public static ClusterHealServiceException TARGET_VC_HOST_NOT_FOUND(String hostName, String vmName) {
      return new ClusterHealServiceException(null, "TARGET_VC_HOST_NOT_FOUND", hostName, vmName);
   }
}
