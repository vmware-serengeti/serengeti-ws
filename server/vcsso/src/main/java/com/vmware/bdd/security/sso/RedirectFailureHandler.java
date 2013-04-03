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
package com.vmware.bdd.security.sso;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class RedirectFailureHandler implements AuthenticationFailureHandler {
   private static final Logger logger = Logger.getLogger(RedirectFailureHandler.class);
   private static final String SSO_TOKEN_LOGIN_URL_ERR_MSG_PARAM_KEY = "LoginErrMsg";
   private static final String SP_LOGIN_URL = "/serengeti";
   private static final String GENERIC_ERROR_MSG = "SSOTokenAuthenticationError";
   public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
         AuthenticationException exception) throws IOException, ServletException {
      logger.debug("sending 401 Unauthorized error");
      String errorMsg = exception.getMessage() != null ? exception.getMessage() : GENERIC_ERROR_MSG;
      String spLoginrlWithErrMsg = composeSpLoginUrlWithErrMsg(SP_LOGIN_URL, "Web SSO Error: " + errorMsg);
      response.sendRedirect(spLoginrlWithErrMsg);
   }

   private String composeSpLoginUrlWithErrMsg(String spLoginrl, String errMsg) {
      String encodedLoginErrMsg = null;
      try {
         encodedLoginErrMsg = URLEncoder.encode(errMsg, "UTF-8");
      } catch (Exception e) {
         logger.error("Compose SSO token login URL error message parameter error!", e);
      }
      StringBuffer spLoginUrlWithErrMsg = new StringBuffer(spLoginrl);
      if (encodedLoginErrMsg != null && !encodedLoginErrMsg.isEmpty()) {
         if (spLoginrl.contains("?")) {
            spLoginUrlWithErrMsg.append("&");
         } else {
            spLoginUrlWithErrMsg.append("?");
         }
         spLoginUrlWithErrMsg.append(SSO_TOKEN_LOGIN_URL_ERR_MSG_PARAM_KEY);
         spLoginUrlWithErrMsg.append("=");
         spLoginUrlWithErrMsg.append(encodedLoginErrMsg);
      }
      return spLoginUrlWithErrMsg.toString();
   }
}