/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.vmware.bdd.security.exception.VCConnectException;

public class NoRedirectFailureHandler implements AuthenticationFailureHandler {

   private static final Logger logger = Logger
         .getLogger(NoRedirectFailureHandler.class);

   public void onAuthenticationFailure(HttpServletRequest request,
         HttpServletResponse response, AuthenticationException exception)
         throws IOException, ServletException {
      if (exception instanceof VCConnectException) {
         logger.debug("sending 500 internal error");
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
               exception.getMessage());
      } else {
         logger.debug("sending 401 Unauthorized error");
         response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
               "Authentication Failed: " + exception.getMessage());
      }
   }
}
