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
package com.vmware.bdd.software.mgmt.plugin.exception;


import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * <code>SoftwareManagementPluginException</code> is the superclass of those
 * exceptions that can be thrown during the normal operation of the software
 * management plugin.
 * <p>
 *
 **/
public class SoftwareManagementPluginException extends RuntimeException {
   private static final long serialVersionUID = -3167102179098077601L;

   private final static Logger LOGGER = Logger.getLogger(SoftwareManagementPluginException.class);

   private static ResourceBundle MSG_BUNDLE = null;

   static {
      try {
         //not allow to select locale for the moment.
         MSG_BUNDLE = ResourceBundle.getBundle("software-mgmt-plugin-errmsg", Locale.getDefault());
      } catch (Exception ex) {
         LOGGER.error("failed to load message bundle: " + ex.getMessage());
      }
   }


   private String errCode;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param cause cause exception
    * @param errCode predefined error code
    * @param details additional details
    */
   public SoftwareManagementPluginException(String errCode, Throwable cause, Object... details) {
      super(formatErrorMessage(errCode, details), cause);
      this.errCode = errCode;
   }

   public String getErrCode() {
      return errCode;
   }

   private static String formatErrorMessage(final String errorId, Object... args) {
      String msg = null;


      try {
         if(MSG_BUNDLE != null) {
            msg = MSG_BUNDLE.getString(errorId);
         } else {
            LOGGER.error("message bundle is null.");
         }
      } catch (Exception ex) {
         LOGGER.error(String.format("ErrorCode (%s) not found in MessageBundle.", errorId), ex);
      }

      if (msg == null) {
         String detailText = args != null && args.length > 0 ? Arrays.toString(args) : "";
         return String.format("<#%s#>, details: %s", errorId, detailText);
      }

      return String.format(msg, args);
   }

   public static SoftwareManagementPluginException APP_MANAGER_COMMON_EXCEPTION(String errCode, Throwable cause, Object... args) {
      return new SoftwareManagementPluginException(errCode, cause, args);
   }

   public static SoftwareManagementPluginException CREATE_CLUSTER_EXCEPTION(Throwable cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.CREATE_CLUSTER_EXCEPTION", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException CREATE_CLUSTER_FAIL(String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.CREATE_CLUSTER_FAIL", null, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException SCALE_OUT_CLUSTER_FAILED(Throwable cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.SCALE_OUT_CLUSTER_FAILED", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException DELETE_NODES_FAILED(Throwable cause, String appMgr, Object[] nodeNames) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.DELETE_NODES_FAILED", cause, appMgr, Arrays.toString(nodeNames));
   }

   public static SoftwareManagementPluginException RECONFIGURE_CLUSTER_FAILED(Throwable cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.RECONFIGURE_CLUSTER_FAILED", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException RETRIEVE_SUPPORTED_STACKS_FAIL(Throwable cause, String appMgr) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.RETRIEVE_SUPPORTED_STACKS_FAIL", cause, appMgr);
   }

   public static SoftwareManagementPluginException DELETE_CLUSTER_FAILED(Throwable cause,String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.DELETE_CLUSTER_FAILED", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException START_CLUSTER_FAILED(Throwable cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.START_CLUSTER_FAILED", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException STOP_CLUSTER_FAILED(String appMgr, String clusterName, String errMsg) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.STOP_CLUSTER_FAILED", null, appMgr, clusterName, errMsg);
   }

   public static SoftwareManagementPluginException STOP_CLUSTER_EXCEPTION(Exception cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.STOP_CLUSTER_EXCEPTION", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException CHECK_SERVICE_FAILED(String clusterName, Throwable cause) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.CHECK_SERVICE_FAILED", cause, clusterName);
   }

   public static SoftwareManagementPluginException CONFIGURE_SERVICE_FAILED(Throwable cause) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.CONFIGURE_SERVICE_FAILED", cause);
   }


   public static SoftwareManagementPluginException START_SERVICE_FAILED(Throwable cause, String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.START_SERVICE_FAILED", cause, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException CLUSTER_ALREADY_EXIST(String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.CLUSTER_ALREADY_EXIST", null, clusterName);
   }

   public static SoftwareManagementPluginException QUERY_CLUSTER_STATUS_FAILED(String clusterName, Throwable cause) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.QUERY_CLUSTER_STATUS_FAILED", cause, clusterName);
   }

   public static SoftwareManagementPluginException UNKNOWN_CERTIFICATE(String cert) {
      return  APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.UNKNOWN_CERTIFICATE", null, cert);
   }

   public static SoftwareManagementPluginException INSTALL_COMPONENTS_FAIL(String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.INSTALL_COMPONENTS_FAIL", null, appMgr, clusterName);
   }

   public static SoftwareManagementPluginException START_CLUSTER_FAILED_NOT_PROV_BY_BDE(String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.START_CLUSTER_FAILED_NOT_PROVISIONED_BY_BDE", null, clusterName);
   }

   public static SoftwareManagementPluginException ADD_CLUSTER_REPORT_FAIL(Throwable cause) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.ADD_CLUSTER_REPORT_FAIL", cause);
   }

   public static SoftwareManagementPluginException UNSURE_CLUSTER_EXIST(String appMgr, String clusterName) {
      return APP_MANAGER_COMMON_EXCEPTION("APP_MANAGER.UNSURE_CLUSTER_EXIST", null, appMgr, clusterName);
   }
}
