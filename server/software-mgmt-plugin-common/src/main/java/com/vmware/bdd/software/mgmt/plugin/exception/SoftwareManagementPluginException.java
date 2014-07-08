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
package com.vmware.bdd.software.mgmt.plugin.exception;


/**
 * <code>SoftwareManagementPluginException</code> is the superclass of those
 * exceptions that can be thrown during the normal operation of the software
 * management plugin.
 * <p>
 * 
 **/
public class SoftwareManagementPluginException extends RuntimeException {
   private static final long serialVersionUID = -3167102179098077601L;
   private String errCode;

   public SoftwareManagementPluginException() {
      super();
   }

   public SoftwareManagementPluginException(String message) {
      this(message, null);
   }

   public SoftwareManagementPluginException(String message, Throwable cause) {
      this("UNKNOWN", message, cause);
   }

   public SoftwareManagementPluginException(String errCode, String message, Throwable cause) {
      super(message, cause);
      this.errCode = errCode;
   }

   public static SoftwareManagementPluginException INVALID_VERSION(String version, Throwable cause) {
      return new SoftwareManagementPluginException("INVALID_VERSION", "Version " + version + " is invalid", cause);
   }

   public static SoftwareManagementPluginException CREATE_CLUSTER_FAILED(String message, Throwable cause) {
      return new SoftwareManagementPluginException("CREATE_CLUSTER_FAILED", message, cause);
   }

   public static SoftwareManagementPluginException DELETE_CLUSTER_FAILED(String clusterName, Throwable cause) {
      return new SoftwareManagementPluginException("DELETE_CLUSTER_FAILED", "Failed to delete cluster " + clusterName, cause);
   }

   public static SoftwareManagementPluginException START_CLUSTER_FAILED(String clusterName, Throwable cause) {
      return new SoftwareManagementPluginException("START_CLUSTER_FAILED", "Failed to start cluster " + clusterName, cause);
   }

   public static SoftwareManagementPluginException STOP_CLUSTER_FAILED(String clusterName, Throwable cause) {
      return new SoftwareManagementPluginException("STOP_CLUSTER_FAILED", "Failed to stop cluster " + clusterName, cause);
   }

   public static SoftwareManagementPluginException CHECK_SERVICE_FAILED(String clusterName, Throwable cause) {
      return new SoftwareManagementPluginException("CHECK_SERVICE_FAILED", "Failed to check service state for cluster " + clusterName, cause);
   }

   public static SoftwareManagementPluginException CONFIGURE_SERVICE_FAILED(String message, Throwable cause) {
      return new SoftwareManagementPluginException("CONFIGURE_SERVICE_FAILED", message, cause);
   }


   public static SoftwareManagementPluginException START_SERVICE_FAILED(String message, Throwable cause) {
      return new SoftwareManagementPluginException("START_SERVICE_FAILED", message, cause);
   }

   public static SoftwareManagementPluginException CLUSTER_ALREADY_EXIST(String clusterName, Throwable cause) {
      return new SoftwareManagementPluginException("CLUSTER_ALREADY_EXIST", "Cluster " + clusterName + " already exist", cause);
   }

   public String getErrCode() {
      return errCode;
   }
}
