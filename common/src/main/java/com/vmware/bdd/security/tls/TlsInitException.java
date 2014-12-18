/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.security.tls;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Created By xiaoliangl on 12/1/14.
 */
public class TlsInitException extends  RuntimeException{
   private static final long serialVersionUID = -3167102179098077601L;

   private final static Logger LOGGER = Logger.getLogger(TlsInitException.class);

   private static ResourceBundle MSG_BUNDLE = null;

   static {
      try {
         //not allow to select locale for the moment.
         MSG_BUNDLE = ResourceBundle.getBundle("com.vmware.bdd.security.tls.messages", Locale.getDefault());
      } catch (Exception ex) {
         LOGGER.error("failed to load message bundle: " + ex.getMessage());
      }
   }


   private String errCode;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param cause cause exception
    * @param errCode predefined error code
    * @param details additional details
    */
   public TlsInitException(String errCode, Throwable cause, Object... details) {
      super(formatErrorMessage(errCode, details), cause);
      this.errCode = errCode;
   }

   public String getErrCode() {
      return errCode;
   }

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
}
