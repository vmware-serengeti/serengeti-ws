/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.rest.advice;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.ServerErrorMessages;
import com.vmware.bdd.exception.WarningMessageException;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

/**
 * default Exception handlers for BddExceptions and unknown exceptions.
 * Created by xiaoliangl on 11/12/15.
 */
@ControllerAdvice
public class DefaultExceptionHandler {
   private static final Logger logger = Logger.getLogger(RestValidationErrorHandler.class);

   private static final String ERR_CODE_FILE = "serengeti-errcode.properties";
   //default error code is 400 BAD REQUEST
   private static final int DEFAULT_HTTP_ERROR_CODE = 400;
   /* HTTP status code read from a config file. */
   private Configuration httpStatusCodes;


   @PostConstruct
   private void init() {
      PropertiesConfiguration config = null;
      try {
         config = new PropertiesConfiguration();
         config.setListDelimiter('\0');
         config.load(ERR_CODE_FILE);
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Cannot load Serengeti error message file.";
         logger.fatal(message, ex);
         throw BddException.APP_INIT_ERROR(ex, message);
      }
      httpStatusCodes = config;
   }

   /**
    * all unknown exceptions are internal server errors.
    * @param t unknown exceptions raised
    * @return Bdd Error Message that will be sent to the client.
    */
   @ExceptionHandler(Throwable.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   @ResponseBody
   public BddErrorMessage processUnhandledException(Throwable t) {
      logger.error("BDE Internal Server Error", t);

      return ServerErrorMessages.fromException(BddException.INTERNAL_SERVER_ERROR());
   }

   /**
    * convert BddException to properly REST Message
    * @param ex BddException raised from server code
    * @param response access to the HTTP response
    * @return REST Message to the client
    */
   @ExceptionHandler(BddException.class)
   @ResponseBody
   public BddErrorMessage processBddException(BddException ex, HttpServletResponse response) {
      //if you want to change the response code, don't forget to add mapping inside serengeti-errcode.properties
      response.setStatus(getHttpErrorCode(ex.getFullErrorId()));

      return ServerErrorMessages.fromException(ex);
   }

   private int getHttpErrorCode(String errorId) {
      return httpStatusCodes.getInteger(errorId, DEFAULT_HTTP_ERROR_CODE);
   }

}
