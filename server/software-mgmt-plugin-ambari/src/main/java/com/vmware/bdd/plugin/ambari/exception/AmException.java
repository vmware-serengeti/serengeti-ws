/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.exception;

import com.vmware.bdd.exception.BddException;

public class AmException extends BddException {

   private static final long serialVersionUID = 5585914528769234047L;

   public AmException() {
   }

   public AmException(String msg) {}

   public AmException(Throwable cause, String errorId, Object... detail) {
      super(cause, "Ambari", errorId, detail);
   }

   public static AmException INVALID_VERSION(String version) {
      return new AmException(null, "INVALID_VERSION", version);
   }

   public static AmException UNSURE_CLUSTER_EXIST(String clusterName) {
      return new AmException(null, "UNSURE_CLUSTER_EXIST", clusterName);
   }

   public static AmException PROVISION_FAILED(String clusterName) {
      return new AmException(null, "PROVISION_FAILED", clusterName);
   }

   public static AmException STOP_SERVICES_FAILED(String clusterName, String serviceName) {
      return null;
   }
   public static AmException PROVISION_WITH_BLUEPRINT_FAILED(String clusterName) {
      return new AmException(null, "PROVISION_WITH_BLUEPRINT_FAILED", clusterName);
   }

   public static AmException BOOTSTRAP_FAILED(String clusterName) {
      return new AmException(null, "BOOTSTRAP_FAILED", clusterName);
   }

   public static AmException BOOTSTRAP_REQUEST_FAILED(Long requestId) {
      return new AmException(null, "BOOTSTRAP_REQUEST_FAILED", requestId);
   }

   public static AmException BOOTSTRAP_ALL_HOSTS_FAILED(Long requestId) {
      return new AmException(null, "BOOTSTRAP_ALL_HOSTS_FAILED", requestId);
   }

   public static AmException CREATE_BLUEPRINT_FAILED(String clusterName) {
      return new AmException(null, "CREATE_BLUEPRINT_FAILED", clusterName);
   }

   public static AmException UNSURE_BLUEPRINT_EXIST(String blueprintName) {
      return new AmException(null, "UNSURE_BLUEPRINT_EXIST", blueprintName);
   }

}
