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

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.utils.Constants;

public class AmbariApiException extends RuntimeException {

   private static final long serialVersionUID = 5585914528766844047L;

   private final static Logger LOGGER = Logger.getLogger(AmbariApiException.class);
   private static ResourceBundle MSG_BUNDLE = null;

   private String errCode;

   static {
      try {
         //not allow to select locale for the moment.
         MSG_BUNDLE = ResourceBundle.getBundle("ambari-api-plugin-errmsg", Locale.getDefault());
      } catch (Exception ex) {
         LOGGER.error("failed to load message bundle: " + ex.getMessage());
      }
   }

   public AmbariApiException(String errCode, Throwable cause, Object... details) {
      super(formatErrorMessage(errCode, details), cause);
      this.errCode = errCode;
   }

   public AmbariApiException() { }

   private static String formatErrorMessage(final String errorId, Object... args) {
      String msg = null;

      try {
         if(MSG_BUNDLE != null) {
            msg = MSG_BUNDLE.getString(errorId);
         } else {
            LOGGER.error("message bundle is null.");
         }
      } catch (Exception ex) {
         LOGGER.error(String.format("ErrorCode (%s) not found in MessageBundle.", errorId), ex);
      }

      if (msg == null) {
         String detailText = args != null && args.length > 0 ? Arrays.toString(args) : "";
         return String.format("<#%s#>, details: %s", errorId, detailText);
      }

      return String.format(msg, args);
   }

   public static AmbariApiException RESPONSE_EXCEPTION(int errCode, String message) {
      return new AmbariApiException("AMBARI_API.RESPONSE_EXCEPTION", null, message);
   }

   public String getErrCode() {
      return errCode;
   }

   public static AmbariApiException CANNOT_CONNECT_AMBARI_SERVER(Throwable cause) {
      Throwable sourceCause = cause;
      while (cause.getCause() != null) {
         cause = cause.getCause();
      }

      if ((Constants.AMBARI_CONNECT_EXCEPTION).equals(cause.getClass().toString())) {
         return new AmbariApiException("AMBARI_API.CANNOT_CONNECT_AMBARI_SERVER", cause, cause.getMessage());
      } else {
         if (cause.getMessage() == null) {
            return new AmbariApiException("AMBARI_API.AMBARI_SERVER_ERROR", sourceCause, sourceCause.getMessage());
         } else {
            return new AmbariApiException("AMBARI_API.AMBARI_SERVER_ERROR", cause, cause.getMessage());
         }
      }
   }
}
