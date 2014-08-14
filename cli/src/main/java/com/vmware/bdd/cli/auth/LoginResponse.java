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
}
