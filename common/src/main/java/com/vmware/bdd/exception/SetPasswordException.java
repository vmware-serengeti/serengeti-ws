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

public class SetPasswordException extends BddException{
   private static final long serialVersionUID = 1l;

   public SetPasswordException() {
   }

   public SetPasswordException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SET_PASSWORD", errorId, detail);
   }

   public static SetPasswordException SETUP_PASSWORDLESS_LOGIN_TIMEOUT(Throwable cause, String nodeIP) {
      return new SetPasswordException(cause, "SETUP_PASSWORDLESS_LOGIN_TIMEOUT", nodeIP);
   }

   public static SetPasswordException FAIL_TO_SETUP_PASSWORDLESS_LOGIN(String nodeIP) {
      return new SetPasswordException(null, "SETUP_PASSWORDLESS_LOGIN_FAILED", nodeIP);
   }

   public static SetPasswordException FAIL_TO_SET_PASSWORD(String nodeIP, String msg) {
      return new SetPasswordException(null, "FAILED_TO_SET_PASSWORD", nodeIP, msg);
   }

   public static SetPasswordException INVALID_PASSWORD(String errMsg) {
      return new SetPasswordException(null, "INVALID_PASSWORD", errMsg);
   }

   public static SetPasswordException FAIL_TO_REMOVE_SSH_LIMIT(String nodeIP) {
      return new SetPasswordException(null, "FAIL_TO_REMOVE_SSH_LIMIT", nodeIP);
   }

   public static SetPasswordException GOT_JSCH_EXCEPTION_WHEN_SET_PASSWORD(
         Throwable cause, String nodeIP) {
      return new SetPasswordException(cause, "GOT_JSCH_EXCEPTION_WHEN_SET_PASSWORD", nodeIP);
   }

   public static SetPasswordException FAIL_TO_SETUP_LOGIN_TTY(String nodeIP, String errMsg) {
      return new SetPasswordException(null, "FAIL_TO_SETUP_LOGIN_TTY", nodeIP, errMsg);
   }
}
