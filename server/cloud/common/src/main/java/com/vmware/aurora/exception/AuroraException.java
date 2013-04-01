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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;

import com.vmware.aurora.buildinfo.BuildInfo;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CommonUtil;

/* ----------------------------------------------------------------------
The "new" aurora exception framework is designed to achieve the following,
    # Consistent look and feel across of backend exceptions.
    # A single location contains all error message text strings,
      so that doc writers can proof read all of them, and, this is
      the single place for localization of error messages.
    # Right now the exception only has a cause (Throwable), a section,
      an errorId and a text message.
    # The scope of this framework is to cover *ALL* backend exceptoins
      that may be thrown to UI.  Feel free to use your own exception
      class/framework but *ONLY IF* you are sure that your exception will
      be caught within backend.  Otherwise UI will print an unfriendly
      message to the users screen and it is automatically considered a
      BUG.

How to add a new exception type.
    # If you need to add a new type of exception, please add it in
      package com.vmware.aurora.common.exception.
    # Add a new SECTION for your exception in aurora-errmsg.properties.
      The SECTION should contains only CAPITAL LETTER and _.
    # Under the section, add your error message.  The "key" of your
      message should be SECION.ERRORID.  The text value of the error
      should be proper English.  You are free to use java string
      formatter %.  The % parameters should only be used to display
      strings that only available from run time context, and, it should
      be readable in *ANY* language.
    # For each error message, you should add a public static method.
      By convention, its name should be ERRORID.  The parameters of
      the method should match the % format in the error message text,
      with an optional leading Throwable param, for exception chaining.
    #
-------------------------------------------------------------------- */

/**
 * All exceptions which should be converted to client side checked exception
 * should be derived from this class or implement AuroraFaultConvertable
 * interface.
 *
 * To convert an AuroraException ex into SOAP customized fault, the method
 * ExceptionUtil.convertToAuroraFaultMessage(ex) should be called.
 */
@SuppressWarnings("serial")
public class AuroraException extends RuntimeException {

   // Default public constructor. This should only be used by AMF client.
   public AuroraException() {}

   private String section;
   private String errorId;
   public String getSection() { return section; }
   public String getErrorId() { return section + "." + errorId; } // XXX: fix after merging with AuroraError
   public String getSimpleErrorId() { return errorId; }
   // Bad bad code.  What use is a message template
   // once it is thrown outside of context/stack?
   // But we have tons of place calling this so here it is.
   public String getErrorMessageTemplate() { return toString(); }

   /* Error messages read from a config file. */
   private static org.apache.commons.configuration.Configuration messages = init();
   private static org.apache.commons.configuration.Configuration init() {
       org.apache.commons.configuration.Configuration config = null;
       try {
           String errmsgfile = Configuration.getString("cms.errmsg_file", "aurora-errmsg.properties");
           config = CommonUtil.GetPropertiesConfiguration(errmsgfile);
       } catch (ConfigurationException ex) {
           // error out if the configuration file is not there
          String message = "Failed to load aurora error message file.";
           Logger.getLogger(Configuration.class).fatal(message, ex);
           throw AuroraException.APP_INIT_ERROR(ex, message);
       }
       return config;
   }

   private static String formatErrorMessage(final String err, Object... args) {
      String msgt = messages.getString(err);
      if (msgt == null) {
         return "Error: Invalid aurora error message Id " + err + ".";
      }
      return String.format(msgt, args);
   }

   protected AuroraException(Throwable cause, String section, String errorId, Object... args) {
      super(formatErrorMessage(section + "." + errorId, args), cause);
      this.section = section;
      this.errorId = errorId;
   }

   /**
    * Check if the given exception is in the appropriate form for the UI. If not,
    * we wrap it in AuroraException.
    * @param exception -- the original exception.
    * @return the original exception, if it is in the appropriate form for the UI
    * (either an AuroraException or a Spring's AccessDeniedException). Otherwise,
    * return this exception wrapped in AuroraException.INTERNAL.
    */
   public static RuntimeException wrapIfNeeded(Throwable exception) {
      if (exception instanceof AuroraException) {
         return (AuroraException)exception;
      } else if (exception instanceof AccessDeniedException) {
         return (AccessDeniedException)exception;
      }
      return INTERNAL(exception);
   }
   public static AuroraException INTERNAL() {
      return INTERNAL(null);
   }
   public static AuroraException INTERNAL(Throwable ex) {
      return new AuroraException(ex, "AURORA", "INTERNAL");
   }
   public static AuroraException NO_PERMISSION() {
      return new AuroraException(null, "AURORA", "NO_PERMISSION");
   }
   public static AuroraException SESSION_ERROR() {
      return new AuroraException(null, "AURORA", "SESSION_ERROR");
   }

   /**
    * This exception is designed to be thrown only when initializing Aurora
    * web application. When this exception is thrown, the web container
    * will catch it and abort the application deployment, which is just what
    * we want. Be sure that all code that can throw this exception will be
    * called during web application initialization.
    */
   public static AuroraException APP_INIT_ERROR(Throwable ex, String detail) {
      return new AuroraException(ex, "AURORA", "APP_INIT_ERROR", detail);
   }

   /**
    * In debug build, return the exception with the current stack trace
    * stitched in front of the original exception stack trace.
    *
    * In non-debug build, set the current stack trace.
    *
    * @param e
    * @return the original exception.
    */
   public static AuroraException stitchStackTraces(AuroraException e) {
      if (BuildInfo.debug) {
         StackTraceElement[] oldFrame = e.getStackTrace();
         StackTraceElement[] curFrame = new Throwable().getStackTrace();
         StackTraceElement[] newFrame = new StackTraceElement[oldFrame.length +
                                                              curFrame.length];
         int i = 0, j = 0;
         for (i = 0; i < curFrame.length; i++) {
            newFrame[j++] = curFrame[i];
         }
         for (i = 0; i < oldFrame.length; i++) {
            newFrame[j++] = oldFrame[i];
         }
         e.setStackTrace(newFrame);
      } else {
         // replace stack trace with those of current thread
         e.fillInStackTrace();
      }
      return e;
   }
}
