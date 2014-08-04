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

import java.util.Arrays;
import java.util.Collections;

public class AmException extends SoftwareManagementPluginException {

   private static final long serialVersionUID = 5585914528769234047L;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public AmException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public static AmException BOOTSTRAP_FAILED(Object[] failedNodes) {
      return failedNodes == null ? new AmException("AMBARI.BOOTSTRAP_FAIL_ALL", null) :
      new AmException("AMBARI.BOOTSTRAP_FAIL", null, Arrays.toString(failedNodes));
   }

   public static AmException BOOTSTRAP_FAILED_EXCEPTION(Exception cause, String clusterDefName) {
      return new AmException("AMBARI.BOOTSTRAP_FAILED_EXCEPTION", cause, clusterDefName);
   }

   /*@unused
   public static AmException CREATION_FAIL_EXCEPTION(Exception cause, String blueprintName) {
      return new AmException("AMBARI.CREATION_FAIL_EXCEPTION", cause, blueprintName);
   }*/

   public static AmException CREATE_BLUEPRINT_FAILED(Throwable cause, String clusterName) {
      return new AmException("AMBARI.CREATE_BLUEPRINT_FAILED", cause, clusterName);
   }

   public static AmException UNSURE_BLUEPRINT_EXIST(String blueprintName) {
      return new AmException("AMBARI.BLUEPRINT_UNSURE_EXIST", null, blueprintName);
   }

   public static AmException BLUEPRINT_ALREADY_EXIST(String blueprintName) {
      return new AmException("AMBARI.BLUEPRINT_ALREADY_EXIST", null, blueprintName);
   }

   public static AmException PROVISION_WITH_BLUEPRINT_FAILED(Throwable cause, String blueprintName) {
      return new AmException("AMBARI.PROVISION_WITH_BLUEPRINT_FAILED", cause, blueprintName);
   }


   public static AmException CLUSTER_NOT_PROVISIONED(String clusterName) {
      return new AmException("AMBARI.CLUSTER_NOT_PROVISIONED", null, clusterName);
   }
}
