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
package com.vmware.aurora.exception;

@SuppressWarnings("serial")
public class CommonException extends AuroraException {
   // Default public constructor. This should only be used by AMF client.
   public CommonException() {}

   private CommonException(Throwable t, String errorId, Object... args) {
      super(t, "COMMON", errorId, args);
   }

   public static CommonException INTERNAL() {
      return new CommonException(null, "INTERNAL_ERROR");
   }
   public static CommonException INTERNAL(Throwable t) {
      return new CommonException(t, "INTERNAL_ERROR");
   }
   public static CommonException WRONG_ARGUMENT() {
      return WRONG_ARGUMENT(null);
   }
   public static CommonException WRONG_ARGUMENT(Throwable t) {
      return new CommonException(t, "WRONG_ARGUMENT");
   }
   public static CommonException NOT_IMPLEMENTED() {
      return new CommonException(null, "NOT_IMPLEMENTED");
   }
   public static CommonException USER_NOT_FOUND(String name) {
      return new CommonException(null, "USER_NOT_FOUND", name);
   }
   public static CommonException DBS_NOT_FOUND(String name) {
      return new CommonException(null, "DBS_NOT_FOUND", name);
   }
   public static CommonException VM_NOT_FOUND(String name) {
      return new CommonException(null, "VM_NOT_FOUND", name);
   }
   public static CommonException EMAIL_FAILURE() {
      return new CommonException(null, "EMAIL_FAILURE");
   }
   public static CommonException VALIDATION_EMAIL_FAILURE() {
      return new CommonException(null, "VALIDATION_EMAIL_FAILURE");
   }
   public static CommonException DB_STATUS_UNSUPPORTED_OP() {
      return new CommonException(null, "DB_STATUS_UNSUPPORTED_OP");
   }

   public static CommonException UNSUPPORTED_ACTION_FOR_DATABASE_TYPE(String operation, String dbType) {
      return new CommonException(null, "UNSUPPORTED_ACTION_FOR_DATABASE_TYPE", operation, dbType);
   }

   public static CommonException UNSUPPORTED_ACTION_FOR_AGENT_VERSION(String operation, String version) {
      return new CommonException(null, "UNSUPPORTED_ACTION_FOR_AGENT_VERSION", operation, version);
   }
}
