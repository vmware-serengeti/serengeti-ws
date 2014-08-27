package com.vmware.bdd.cli.auth;

import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.utils.CommonUtil;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Http Login Client
 */
@Component
public class LoginClientImpl {
   private final static Logger LOGGER = Logger.getLogger(LoginClientImpl.class);
   protected static final String SET_COOKIE_HEADER = "Set-Cookie";

   private HttpClient client1 = new HttpClient();

   /**
    *
    * attempt login by posting credentials to serengeti server
    *
    * @param serengetiURL https://host:8443/serengeti/api/
    * @param userName vc user name
    * @param password vc password
    * @throws IOException connection exception
    */
   public LoginResponse login(final String serengetiURL, String userName, String password) throws IOException {
      String url = serengetiURL + Constants.REST_PATH_LOGIN;
      PostMethod loginPost = new PostMethod(url);

      //handling non-ascii username and password. Encoding by Apache HTTP Client.
      loginPost.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      NameValuePair[] loginCredentials = new NameValuePair[] {
            new NameValuePair("j_username", userName) ,
            new NameValuePair("j_password", password)
      };

      loginPost.setRequestBody(loginCredentials);

//      client1.getHostConfiguration().setProxy("127.0.0.1", 8810);

      int responseCode = client1.executeMethod(loginPost);

      LOGGER.debug("resp code is: " + responseCode);

      LoginResponse loginResponse;
      if (responseCode == org.apache.commons.httpclient.HttpStatus.SC_OK) {
         //normal response
         String cookieValue = null;
         Header setCookieHeader = loginPost.getResponseHeader(SET_COOKIE_HEADER);

         if(setCookieHeader != null) {
            cookieValue = setCookieHeader.getValue();

            if (!CommonUtil.isBlank(cookieValue) && cookieValue.contains(";")) {
               cookieValue = cookieValue.split(";")[0];
            }
         }

         loginResponse = new LoginResponse(responseCode, cookieValue);
      } else {
         loginResponse = new LoginResponse(responseCode, null);
      }

      return loginResponse;
   }
}
