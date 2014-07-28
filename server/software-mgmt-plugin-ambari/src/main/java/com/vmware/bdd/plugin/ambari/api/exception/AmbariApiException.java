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
package com.vmware.bdd.plugin.ambari.api.exception;


public class AmbariApiException extends RuntimeException {

   private static final long serialVersionUID = 5585914528766844047L;

   private String errCode;

   public AmbariApiException() {
   }

   public AmbariApiException(String msg) {
   }

   public AmbariApiException(String errCode, String message, Throwable cause) {
      super(message, cause);
      this.errCode = errCode;
   }

   public static AmbariApiException RESPONSE_EXCEPTION(int errCode, String message) {
      return new AmbariApiException(String.valueOf(errCode), message, null);
   }

   public String getErrCode() {
      return errCode;
   }
}
