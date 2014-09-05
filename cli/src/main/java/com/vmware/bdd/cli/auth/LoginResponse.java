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
package com.vmware.bdd.cli.auth;

/**
 * Http Login Response
 */
public class LoginResponse {
   private int responseCode;
   private String sessionId;

   public LoginResponse(int responseCode, String sessionId) {
      this.responseCode = responseCode;
      this.sessionId = sessionId;
   }

   /**
    *
    * @return HTTP response code
    */
   public int getResponseCode() {
      return responseCode;
   }

   /**
    * successful login will have session id; otherwise is null.
    * @return session id
    */
   public String getSessionId() {
      return sessionId;
   }

   public String toString() {
      return String.format("responseCode:%1s;sessionId:%2s", responseCode, sessionId);
   }

}
