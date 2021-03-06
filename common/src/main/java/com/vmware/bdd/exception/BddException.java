/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.vmware.bdd.apitypes.NetworkDnsType;

import java.util.List;
import java.util.Locale;

public class BddException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   private static MessageSource messageSource;

   static {
      messageSource =
            new ClassPathXmlApplicationContext(
                  "META-INF/spring/commons-context.xml");
   }

   private String section;
   private String errorId;

   public String getSection() {
      return section;
   }

   public String getErrorId() {
      return errorId;
   }

   public String getFullErrorId() {
      return section + "." + errorId;
   }

   protected static String formatErrorMessage(final String errorId,
         Object... args) {
      String msg = messageSource.getMessage(errorId, args, Locale.getDefault());
      if (msg == null) {
         return "Error: Invalid Serengeti error message Id " + errorId;
      }
      return String.format(msg, args);
   }

   protected static String getErrorMessage(final String errorId, Object... args) {
      String msg = messageSource.getMessage(errorId, args, Locale.getDefault());
      return msg;
   }

   public BddException() {
      super();
   }

   public BddException(String msg) {
      super(msg);
   }

   public BddException(Throwable cause, String section, String errorId,
         Object... detail) {
      super(formatErrorMessage(section + "." + errorId, detail), cause);
      this.section = section;
      this.errorId = errorId;
   }

   public static BddException wrapIfNeeded(Throwable exception, String details) {
      if (exception instanceof BddException) {
         return (BddException) exception;
      } else {
         return INTERNAL(exception, details);
      }
   }

   public static BddException INTERNAL(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "INTERNAL_ERROR", detail);
   }


   public static BddException UPGRADE(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "UPGRADE_ERROR", detail);
   }

   public static BddException ExecCommand(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "EXECUTE_COMMAND_ERROR", detail);
   }

   public static BddException VC_EXCEPTION(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "VC_EXCEPTION", detail);
   }

   /**
    * This exception is designed to be thrown only when initializing serengeti
    * web application. When this exception is thrown, the web container will
    * catch it and abort the application deployment, which is just what we want.
    * Be sure that all code that can throw this exception will be called during
    * web application initialization.
    */
   public static BddException APP_INIT_ERROR(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "APP_INIT_ERROR", detail);
   }

   public static BddException NOT_FOUND(Throwable ex, String object,
         String objectName) {
      return new BddException(ex, "BDD", "NOT_FOUND", object, objectName);
   }

   public static BddException NOT_FOUND(String object, String objectName) {
      return NOT_FOUND(null, object, objectName);
   }

   public static BddException NOT_ALLOWED_SCALING(String object,
      String objectName) {
      return new BddException(null, "BDD", "NOT_ALLOWED_SCALING", object,
            objectName);
   }

   public static BddException ALREADY_EXISTS(Throwable ex, String object,
         String objectName) {
      return new BddException(ex, "BDD", "ALREADY_EXISTS", object, objectName);
   }

   public static BddException PG_REFERENCED_MULTI_TIMES() {
      return new BddException(null, "BDD", "PG_REFERENCED_MULTI_TIMES");
   }

   public static BddException MULTI_NETWORKS_FOR_MAPR_DISTRO() {
      return new BddException(null, "BDD", "MULTI_NETWORKS_FOR_MAPR_DISTRO");
   }

   public static BddException ALREADY_EXISTS(String object, String objectName) {
      return ALREADY_EXISTS(null, object, objectName);
   }

   public static BddException INVALID_PARAMETER(Throwable ex, String field,
         Object value) {
      return new BddException(ex, "BDD", "INVALID_PARAMETER", field, value);
   }

   public static BddException INVALID_PARAMETER_WITHOUT_EQUALS_SIGN(Throwable ex,
         String field, Object value) {
      return new BddException(ex, "BDD",
            "INVALID_PARAMETER_WITHOUT_EQUALS_SIGN", field, value);
   }

   public static BddException INVALID_PARAMETER(String field, Object value) {
      return INVALID_PARAMETER(null, field, value);
   }

   public static BddException INVALID_PARAMETER_WITHOUT_EQUALS_SIGN(
         String field, Object value) {
      return INVALID_PARAMETER_WITHOUT_EQUALS_SIGN(null, field, value);
   }

   public static BddException MISSING_PARAMETER(List<String> missingParameters) {
      return new BddException(null, "BDD", "MISSING_PARAMETER", StringUtils.join(missingParameters, ", "));
   }

   public static BddException BAD_REST_CALL(Throwable ex, String reason) {
      return new BddException(ex, "BDD", "BAD_REST_CALL", reason);
   }

   public static BddException VM_NAME_VIOLATE_NAME_PATTERN(String vmName) {
      return new ClusteringServiceException(null, "BDD",
            "VM_NAME_VIOLATE_NAME_PATTERN", vmName);
   }

   public static BddException INVALID_MIN_COMPUTE_NODE_NUM(String minComputeNodeNum,
         String deployedComputeNodeNum, String maxComputeNodeNum) {
      return new BddException(null, "BDD", "INVALID_MIN_COMPUTE_NODE_NUM", minComputeNodeNum,
            deployedComputeNodeNum, maxComputeNodeNum);
   }

   public static BddException INVALID_MAX_COMPUTE_NODE_NUM(String maxComputeNodeNum,
         String deployedComputeNodeNum, String minComputeNodeNum) {
      return new BddException(null, "BDD", "INVALID_MAX_COMPUTE_NODE_NUM", maxComputeNodeNum,
            deployedComputeNodeNum, minComputeNodeNum);
   }

   public static BddException INVALID_TARGET_COMPUTE_NODE_NUM(String targetComputeNodeNum,
         String deployedComputeNodeNum) {
      return new BddException(null, "BDD", "INVALID_TARGET_COMPUTE_NODE_NUM", targetComputeNodeNum,
            deployedComputeNodeNum);
   }

   public static BddException INIT_VC_FAIL() {
      return new BddException(null, "BDD", "INIT_VC_FAIL");
   }

   public static BddException UNSUPPORTED_OPS(String ops, String appMgr) {
      return new BddException(null, "BDD", "UNSUPPORTED_OPS_FOR_APP_MGR", ops, appMgr);
   }

   public static BddException APP_MGR_TYPE_IS_BLANK(String appMgrName) {
      return new BddException(null, "SOFTWARE_MANAGER_COLLECTOR", "APP_MGR_TYPE_IS_BLANK", appMgrName);
   }

   public static BddException CLUSTER_HAS_NO_APP_MGR(String clusterName) {
      return new BddException(null, "CLUSTER", "CLUSTER_HAS_NO_APP_MGR", clusterName);
   }

   public static BddException APP_MGR_NOT_FOUND(String appMgrName) {
      return new BddException(null, "SOFTWARE_MANAGER_COLLECTOR", "APP_MGR_NOT_FOUND", appMgrName);
   }

   public static BddException EXTRA_PACKAGES_NOT_FOUND(String extraPackages) {
      return new BddException(null, "CLUSTER", "EXTRA_PACKAGES_NOT_FOUND", extraPackages);
   }

   public static BddException NEW_RP_EXCLUDE_OLD_RP(Throwable ex, String oldNames,
         String newNames) {
      return new BddException(ex, "CLUSTER_UPDATE", "NEW_RP_EXCLUDE_OLD_RP", oldNames, newNames);
   }

   public static BddException INVALID_DNS_TYPE(NetworkDnsType dnsType) {
      return new BddException(null, "NETWORK", "INVALID_DNS_TYPE", dnsType.toString());
   }

   public static BddException INVALID_OPTIONS_WHEN_UPDATE_NETWORK(String[] options) {
      return new BddException(null, "NETWORK", "INVALID_OPTIONS", StringUtils.join(options, ", "));
   }

   public static BddException FAILED_TO_GENERATE_HOSTNAME(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "GENERATING_HOSTNAME_ERROR", detail);
   }

   public static BddException FAILED_TO_GENERATE_SCRIPT_FOR_UPDATING_ETC_HOSTS(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "FAILED_TO_GENERATE_SCRIPT_FOR_UPDATING_ETC_HOSTS", detail);
   }

   public static BddException SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_NOT_FOUND() {
      return new BddException(null, "BDD", "SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_NOT_FOUND");
   }

   public static BddException SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_READ_ERR(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_READ_ERR", detail);
   }

   public static BddException ACCESS_NOT_ALLOWED_IN_MAINTENANCE_MODE() {
      return new BddException(null, "BDD", "ACCESS_NOT_ALLOWED_IN_MAINTENANCE_MODE");
   }

   public static BddException SYSTEM_DISK_NOT_FOUND_ON_NODE(String nodeName) {
      return new BddException(null, "BDD", "SYSTEM_DISK_NOT_FOUND_ON_NODE", nodeName);
   }

   public static BddException CLUSTER_RECOVER_FAILED(String vmNames) {
      return new BddException(null, "CLUSTER", "RECOVER_FAILED", vmNames);
   }

   public static BddException INTERNAL_SERVER_ERROR() {
      return INTERNAL(null, "UNKNOWN.");
   }
}
