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

package com.vmware.bdd.exception;

import java.util.List;

import com.vmware.bdd.utils.CommonUtil;

public class SoftwareManagerCollectorException extends BddException {
   private static final long serialVersionUID = 1l;

   public SoftwareManagerCollectorException() {
   }

   public SoftwareManagerCollectorException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SOFTWARE_MANAGER_COLLECTOR", errorId, detail);
   }

   public static SoftwareManagerCollectorException DUPLICATE_NAME(String name) {
      return new SoftwareManagerCollectorException(null, "DUPLICATE_NAME", name);
   }

   public static SoftwareManagerCollectorException CLASS_NOT_FOUND_ERROR(
         Exception ex, String className) {
      return new SoftwareManagerCollectorException(ex, "CLASS_NOT_FOUND_ERROR",
            className);
   }

   public static SoftwareManagerCollectorException CAN_NOT_INSTANTIATE(
         Exception ex, String name) {
      return new SoftwareManagerCollectorException(ex, "CAN_NOT_INSTANTIATE",
            name);
   }

   public static SoftwareManagerCollectorException ILLEGAL_ACCESS(Exception ex,
         String name) {
      return new SoftwareManagerCollectorException(ex, "ILLEGAL_ACCESS", name);
   }

   public static SoftwareManagerCollectorException ECHO_FAILURE(String name) {
      return new SoftwareManagerCollectorException(null, "ECHO_FAILURE", name);
   }

   public static SoftwareManagerCollectorException CONNECT_FAILURE(String name,
         String errorMsg) {
      return new SoftwareManagerCollectorException(null, "CONNECT_FAILURE",
            name, errorMsg);
   }

   public static SoftwareManagerCollectorException CLASS_NOT_DEFINED(String name) {
      return new SoftwareManagerCollectorException(null, "CLASS_NOT_DEFINED",
            name);
   }

   public static SoftwareManagerCollectorException PRIVATE_KEY_NOT_FOUND(
         Exception ex, String privateKeyFilePath) {
      return new SoftwareManagerCollectorException(ex, "PRIVATE_KEY_NOT_FOUND",
            privateKeyFilePath);
   }

   public static SoftwareManagerCollectorException PRIVATE_KEY_READ_ERROR(
         Exception ex, String privateKeyFilePath) {
      return new SoftwareManagerCollectorException(ex, "PRIVATE_KEY_READ_ERROR",
            privateKeyFilePath);
   }

   public static SoftwareManagerCollectorException CAN_NOT_DELETE(
         String appManagerName, List<String> clusters) {
      return new SoftwareManagerCollectorException(null, "CAN_NOT_DELETE",
            appManagerName, clusters);
   }

   public static SoftwareManagerCollectorException APPMANAGER_NOT_FOUND(
         String appManagerName) {
      return new SoftwareManagerCollectorException(null, "APPMANAGER_NOT_FOUND",
            appManagerName);
   }

   public static SoftwareManagerCollectorException CAN_NOT_DELETE_DEFAULT() {
      return new SoftwareManagerCollectorException(null, "CAN_NOT_DELETE_DEFAULT");
   }

   public static SoftwareManagerCollectorException CAN_NOT_MODIFY_DEFAULT() {
      return new SoftwareManagerCollectorException(null, "CAN_NOT_MODIFY_DEFAULT");
   }

   public static SoftwareManagerCollectorException INVALID_URL(
         List<String> errorMsgs) {
      return new SoftwareManagerCollectorException(null, "INVALID_URL",
            CommonUtil.mergeErrorMsgList(errorMsgs));
   }

   public static SoftwareManagerCollectorException INVALID_VERSION(String type, String minSupportedVersion, String version) {
      return new SoftwareManagerCollectorException(null, "INVALID_VERSION", type, minSupportedVersion, version);
   }
   public static SoftwareManagerCollectorException BAD_CERT(Throwable throwable) {
      return new SoftwareManagerCollectorException(throwable, "BAD_CERT");
   }
}
