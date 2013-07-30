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
public class GuestVariableException extends AuroraException {
   // Default public constructor. This should only be used by AMF client.
   public GuestVariableException() {}

   private GuestVariableException(Throwable t, String errorId, Object... args) {
      super(t, "GUESTVAR", errorId, args);
   }

   public static final GuestVariableException RETURN_CODE_ERROR(String err) {
      return new GuestVariableException(null, "RETURN_CODE_ERROR", err);
   }

   public static final GuestVariableException COMMUNICATION_ERROR(Throwable t) {
      return new GuestVariableException(t, "COMMUNICATION_ERROR");
   }

   public static final GuestVariableException POWERED_OFF() {
      return new GuestVariableException(null, "POWERED_OFF");
   }

   public static final GuestVariableException TIMEOUT() {
      return new GuestVariableException(null, "TIMEOUT");
   }
}
