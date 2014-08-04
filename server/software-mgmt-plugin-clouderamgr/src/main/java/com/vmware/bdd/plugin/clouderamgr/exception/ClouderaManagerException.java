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
package com.vmware.bdd.plugin.clouderamgr.exception;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 1:13 PM
 */
public class ClouderaManagerException extends SoftwareManagementPluginException {
   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public ClouderaManagerException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public final static ClouderaManagerException GET_SUPPORT_CONFIGS_EXCEPTION(Exception cause) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.GET_SUPPORT_CONFIGS_EXCEPTION", cause);
   }

   public static ClouderaManagerException INSTALL_AGENTS_FAIL(Throwable cause) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.INSTALL_AGENTS_FAIL", cause);
   }

   public static ClouderaManagerException DOWNLOAD_PARCEL_FAIL(String product, String version, String refMsg) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.DOWNLOAD_PARCEL_FAIL", null, product, version, refMsg);
   }

   public static ClouderaManagerException DISTRIBUTE_PARCEL_FAIL(String product, String version,  String refMsg) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.DISTRIBUTE_PARCEL_FAIL", null, product, version, refMsg);
   }

   public static ClouderaManagerException ACTIVATE_PARCEL_FAIL(String product, String version,  String refMsg) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.ACTIVATE_PARCEL_FAIL", null, product, version, refMsg);
   }

   public static ClouderaManagerException DEPROVISION_EXCEPTION(Exception cause, String clusterName) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.DEPROVISION_EXCEPTION", cause, clusterName);
   }

   public static ClouderaManagerException CHECK_CONFIGURED_EXCEPTION(Exception cause, String clusterName) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.CHECK_CONFIGURED_EXCEPTION", cause, clusterName);
   }

   public static ClouderaManagerException INIT_EXCEPTION(Exception cause, String clusterName) {
      return new ClouderaManagerException("CLOUDERA_MANAGER.INIT_EXCEPTION", cause, clusterName);
   }

   public static ClouderaManagerException FAIL_FETCH_CM_SERVER_HOST_ID() {
      return new ClouderaManagerException("CLOUDERA_MANAGER.FAIL_FETCH_CM_SERVER_HOST_ID", null);
   }
}
