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

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

public class AmException extends SoftwareManagementPluginException {

   private static final long serialVersionUID = 5585914528769234047L;

   public AmException() {
   }

   public AmException(String msg) {
   }

   public AmException(String errCode, String message, Throwable cause) {
      super(errCode, message, cause);
   }

   public static AmException UNSURE_CLUSTER_EXIST(String clusterName) {
      return new AmException("UNSURE_CLUSTER_EXIST", "cluster " + clusterName
            + " not sure exist", null);
   }

   public static AmException STOP_SERVICES_FAILED(String clusterName,
         String serviceName) {
      return null;
   }

   public static AmException BOOTSTRAP_FAILED(String message, Throwable cause) {
      return new AmException("BOOTSTRAP_FAILED", message, cause);
   }

   public static AmException CREATE_BLUEPRINT_FAILED(String message,
         Throwable cause) {
      return new AmException("CREATE_BLUEPRINT_FAILED", message, cause);
   }

   public static AmException UNSURE_BLUEPRINT_EXIST(String blueprintName) {
      return new AmException("UNSURE_BLUEPRINT_EXIST", "blueprint "
            + blueprintName + " not sure exist", null);
   }

   public static AmException BLUEPRINT_ALREADY_EXIST(String blueprintName) {
      return new AmException("BLUEPRINT_ALREADY_EXIST", "blueprint "
            + blueprintName + " already exist", null);
   }

   public static AmException PROVISION_WITH_BLUEPRINT_FAILED(String message,
         Throwable cause) {
      return new AmException("PROVISION_WITH_BLUEPRINT_FAILED", message, cause);
   }

}
