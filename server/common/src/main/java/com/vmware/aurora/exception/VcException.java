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
public class VcException extends AuroraException {
   // Default public constructor. This should only be used by AMF client.
   public VcException() {}

   private VcException(Throwable t, String errorId, Object ... args) {
      super(t, "VC", errorId, args);
   }

   public static VcException INIT_ERROR() {
      return new VcException(null, "INIT_ERROR");
   }
   public static VcException UNAVAILABLE_ERROR(Throwable t) {
      return new VcException(t, "UNAVAILABLE_ERROR",
            t.getMessage() == null ? "" : t.getMessage());
   }
   public static VcException SHUTDOWN_ERROR() {
      return new VcException(null, "SHUTDOWN_ERROR");
   }
   public static VcException UPLOAD_ERROR(Throwable t) {
      return new VcException(t, "UPLOAD_ERROR");
   }
   public static VcException GENERAL_ERROR(Throwable t) {
      return new VcException(t, "GENERAL_ERROR",
            t.getMessage() == null ? "" : t.getMessage());
   }
   public static VcException LOGIN_ERROR(Throwable t) {
      return new VcException(t, "LOGIN_ERROR");
   }
   public static VcException LOGIN_ERROR() {
      return new VcException(null, "LOGIN_ERROR");
   }
   public static VcException UNSUPPORTED_VERSION(Throwable t, String version) {
      return new VcException(t, "UNSUPPORTED_VERSION", version);
   }
   public static VcException CONNECTING_TO_HOSTD() {
      return new VcException(null, "CONNECTING_TO_HOSTD");
   }
   public static VcException CONNECTING_TO_INVALID_PRODUCT() {
      return new VcException(null, "CONNECTING_TO_INVALID_PRODUCT");
   }
   public static VcException INVALID_ARGUMENT() {
      return new VcException(null, "INVALID_ARGUMENT");
   }
   public static VcException PERFORMANCE_ERROR() {
      return new VcException(null, "PERFORMANCE_ERROR");
   }
   public static VcException SETTING_ERROR() {
      return new VcException(null, "SETTING_ERROR");
   }
   public static VcException INTERNAL_DISK_DETACHMENT_ERROR() {
      return new VcException(null, "INTERNAL_DISK_DETACHMENT_ERROR");
   }
   public static VcException GUEST_TIMEOUT() {
      return new VcException(null, "GUEST_TIMEOUT");
   }
   public static VcException INVALID_MOREF(String id) {
      return new VcException(null, "INVALID_MOREF", id);
   }
   public static VcException MOREF_NOT_READY(String id) {
      return new VcException(null, "MOREF_NOT_READY", id);
   }
   public static VcException UNSUPPORTED_CONTROLLER_TYPE(String controllerType) {
      return new VcException(null, "UNSUPPORTED_CONTROLLER_TYPE", controllerType);
   }
   public static VcException CONTROLLER_NOT_FOUND(String deviceId) {
      return new VcException(null, "CONTROLLER_NOT_FOUND", deviceId);
   }
   public static VcException DISK_NOT_FOUND(String deviceId) {
      return new VcException(null, "DISK_NOT_FOUND", deviceId);
   }

   public static VcException POWER_ON_VM_FAILED(Throwable t, String vmName,
         String message) {
      return new VcException(t, "POWER_ON_VM_FAILED", vmName, message);
   }

   public static VcException POWER_OFF_VM_FAILED(Throwable t, String vmName,
         String message) {
      return new VcException(t, "POWER_OFF_VM_FAILED", vmName, message);
   }

   public static VcException DELETE_VM_FAILED(Throwable t, String vmName,
         String message) {
      return new VcException(t, "DELETE_VM_FAILED", vmName, message);
   }

   public static VcException CREATE_VM_FAILED(Throwable t, String vmName,
         String message) {
      return new VcException(t, "CREATE_VM_FAILED", vmName, message);
   }

   public static VcException CONFIG_VM_FAILED(Throwable t, String vmName,
         String message) {
      return new VcException(t, "CONFIG_VM_FAILED", vmName, message);
   }

   public boolean isINVALID_MOREF() {
      return getSimpleErrorId().equals("INVALID_MOREF");
   }

   public boolean isMOREF_NOTREADY() {
      return getSimpleErrorId().equals("MOREF_NOT_READY");
   }

   /**
    * Returns true for a wrapped exception that indicates a vc unavailability
    * condition (including our failure to login for any reason).
    * @param e exception to check
    * @return true if vc is not available to us
    */
   public static boolean isVcAvailabilityException(Throwable e) {
      if (e instanceof VcException) {
         VcException ex = (VcException)e;
         String errIds[] = {"INIT_ERROR", "UNAVAILABLE_ERROR", "LOGIN_ERROR"};
         for (String errId : errIds) {
            if (errId.equals(ex.getErrorId())) {
               return true;
            }
         }
      }
      return false;
   }
}
