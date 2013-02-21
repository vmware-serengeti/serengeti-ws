/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
package com.vmware.bdd.exception;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class BddException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   private static final String ERR_MSG_FILE = "serengeti-errmsg.properties";

   private String section;
   private String errorId;

   public String getSection() {
      return section;
   }

   public String getErrorId() {
      return errorId;
   }

   public String getFullErrorId() {
      return section + "." + errorId;
   }

   // Error messages read from a config file
   private static org.apache.commons.configuration.Configuration messages =
         init();

   private static org.apache.commons.configuration.Configuration init() {
      PropertiesConfiguration config = null;
      try {
         config = new PropertiesConfiguration();
         config.setListDelimiter('\0');
         config.load(ERR_MSG_FILE);
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Failed to load serengeti error message file.";
         Logger.getLogger(BddException.class).fatal(message, ex);
         throw BddException.APP_INIT_ERROR(ex, message);
      }
      return config;
   }

   private static String formatErrorMessage(final String errorId,
         Object... args) {
      String msg = messages.getString(errorId);
      if (msg == null) {
         return "Error: Invalid aurora error message Id " + errorId;
      }
      return String.format(msg, args);
   }

   public BddException() {
      super();
   }

   public BddException(Throwable cause, String section, String errorId,
         Object... detail) {
      super(formatErrorMessage(section + "." + errorId, detail), cause);
      this.section = section;
      this.errorId = errorId;
   }

   public static BddException wrapIfNeeded(Throwable exception, String details) {
      if (exception instanceof BddException) {
         return (BddException) exception;
      } else {
         return INTERNAL(exception, details);
      }
   }

   public static BddException INTERNAL(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "INTERNAL_ERROR", detail);
   }

   /**
    * This exception is designed to be thrown only when initializing serengeti web
    * application. When this exception is thrown, the web container will catch
    * it and abort the application deployment, which is just what we want. Be
    * sure that all code that can throw this exception will be called during web
    * application initialization.
    */
   public static BddException APP_INIT_ERROR(Throwable ex, String detail) {
      return new BddException(ex, "BDD", "APP_INIT_ERROR", detail);
   }

   public static BddException NOT_FOUND(Throwable ex, String object, String objectName) {
      return new BddException(ex, "BDD", "NOT_FOUND", object, objectName);
   }

   public static BddException NOT_FOUND(String object, String objectName) {
      return NOT_FOUND(null, object, objectName);
   }

   public static BddException ALREADY_EXISTS(Throwable ex, String object, String objectName) {
      return new BddException(ex, "BDD", "ALREADY_EXISTS", object, objectName);
   }

   public static BddException ALREADY_EXISTS(String object, String objectName) {
      return ALREADY_EXISTS(null, object, objectName);
   }

   public static BddException INVALID_PARAMETER(Throwable ex, String field,
         Object value) {
      return new BddException(ex, "BDD", "INVALID_PARAMETER", field, value);
   }

   public static BddException INVALID_PARAMETER(String field, Object value) {
      return INVALID_PARAMETER(null, field, value);
   }

   public static BddException BAD_REST_CALL(Throwable ex, String reason) {
      return new BddException(ex, "BDD", "BAD_REST_CALL", reason);
   }
}
