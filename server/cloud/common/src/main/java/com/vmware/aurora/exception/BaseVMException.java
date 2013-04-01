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

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class BaseVMException extends AuroraException {

   public BaseVMException() {}
   public BaseVMException(Throwable t, String errorId, Object... args) {
      super(t, "BASEVM", errorId, args);
   }

   public static BaseVMException BASE_VM_NOT_FOUND() {
      return new BaseVMException(null, "BASE_VM_NOT_FOUND");
   }

   public static BaseVMException INVALID_BASE_VM() {
      return new BaseVMException(null, "INVALID_BASE_VM");
   }

   public static BaseVMException INVALID_STATUS(Object... expectedStatus) {
      return new BaseVMException(null, "INVALID_STATUS", StringUtils.join(expectedStatus, " or "));
   }

   public static BaseVMException INVALID_SCSI_CONTROLLER(String controllerType) {
      return new BaseVMException(null, "INVALID_SCSI_CONTROLLER", controllerType);
   }

   public static BaseVMException INVALID_FILE_PATH(Throwable t, String path) {
      return new BaseVMException(t, "INVALID_FILE_PATH", path);
   }

   public static BaseVMException CANNOT_UPLOAD_FILE_TO_DATASTORE(Throwable t, String fileSrc,
         String fileDest) {
      return new BaseVMException(t, "CANNOT_UPLOAD_FILE_TO_DATASTORE", fileSrc, fileDest);
   }

   public static BaseVMException CANNOT_DELETE_FILE_FROM_DATASTORE(Throwable t, String file) {
      return new BaseVMException(t, "CANNOT_DELETE_FILE_FROM_DATASTORE", file);
   }

   public static BaseVMException EASY_INSTALL_NOT_SUPPORTED(String osType, String dbVersion) {
      return new BaseVMException(null, "EASY_INSTALL_NOT_SUPPORTED", osType, dbVersion);
   }

   public static BaseVMException CANNOT_FIND_IDE_CONTROLLER() {
      return new BaseVMException(null, "CANNOT_FIND_IDE_CONTROLLER");
   }

   public static BaseVMException INVAILD_BASE_VM_NAME() {
      return new BaseVMException(null, "INVAILD_BASE_VM_NAME");
   }

   public static BaseVMException UNATTENDED_INSTALLATION_FAILED(String vmName, String msg) {
      return new BaseVMException(null, "UNATTENDED_INSTALLATION_FAILED", vmName, msg);
   }
}
