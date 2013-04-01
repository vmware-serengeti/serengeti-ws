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
package com.vmware.bdd.software.mgmt.exception;

import com.vmware.bdd.exception.BddException;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public class SoftwareManagementException extends BddException {

   /**
    * 
    */
   private static final long serialVersionUID = 907209185029966064L;


   public SoftwareManagementException() {
   }

   public SoftwareManagementException(Throwable cause, String errorId,
         Object... objects) {
      super(cause, "SOFTWARE_MANAGEMENT", errorId, objects);
   }

   public static SoftwareManagementException CONNECT_THRIFT_SERVER_FAILURE(
         Throwable t) {
      return new SoftwareManagementException(t,
            "CONNECT_THRIFT_SERVER_FAILURE", new Object[0]);
   }

   public static SoftwareManagementException CLUSTER_OPERATIOIN_FAILURE(
         Throwable t, String clusterName, String operation, String errorMessage) {
      return new SoftwareManagementException(t, "CLUSTER_OPERATIOIN_FAILURE",
            operation, clusterName, errorMessage);
   }

   public static SoftwareManagementException CLUSTER_OPERATIOIN_UNKNOWN_ERROR(
         Throwable t, String clusterName, String operation) {
      return new SoftwareManagementException(t,
            "CLUSTER_OPERATIOIN_UNKNOWN_ERROR", operation, clusterName);
   }

   public static SoftwareManagementException GET_OPERATIOIN_STATUS_FAILURE(
         Throwable t, String clusterName, String errorMessage) {
      return new SoftwareManagementException(t,
            "GET_OPERATIOIN_STATUS_FAILURE", clusterName, errorMessage);
   }

   public static SoftwareManagementException GET_OPERATIOIN_STATUS_UNKNOWN_ERROR(
         Throwable t, String clusterName) {
      return new SoftwareManagementException(t,
            "GET_OPERATIOIN_STATUS_UNKNOWN_ERROR", clusterName);
   }
}
