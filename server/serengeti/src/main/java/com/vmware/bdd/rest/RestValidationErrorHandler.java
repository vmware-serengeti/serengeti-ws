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
package com.vmware.bdd.rest;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.vmware.bdd.exception.WarningMessageException;

import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 11/26/14.
 */
@ControllerAdvice
public class RestValidationErrorHandler {
   private static final Logger logger = Logger.getLogger(RestValidationErrorHandler.class);
   private MessageSource messageSource;

   @Autowired
   public RestValidationErrorHandler(MessageSource messageSource) {
      this.messageSource = messageSource;
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ResponseBody
   public BddErrorMessage processValidationError(MethodArgumentNotValidException ex) {
      BindingResult result = ex.getBindingResult();
      List<FieldError> fieldErrors = result.getFieldErrors();

      BddErrorMessage validationErrMsg = new BddErrorMessage("BDD.INVALID_PARAMS", "");
      validationErrMsg.setErrors(processFieldErrors(fieldErrors).getErrors());
      return validationErrMsg;
   }

   @ExceptionHandler(ValidationException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ResponseBody
   public BddErrorMessage processValidationError(ValidationException ex) {
      BddErrorMessage validationErrMsg = new BddErrorMessage(ex.getFullErrorId(), ex.getMessage());
      validationErrMsg.setErrors(ex.getErrors());

      return validationErrMsg;
   }

   @ExceptionHandler(WarningMessageException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ResponseBody
   public BddErrorMessage processWarningMessage(WarningMessageException ex) {
      BddErrorMessage validationErrMsg = new BddErrorMessage(ex.getFullErrorId(),ex.getMessage());
      validationErrMsg.setWarning(true);
      return validationErrMsg;
   }

   @ExceptionHandler(UntrustedCertificateException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ResponseBody
   public BddErrorMessage processUntrustedCertificate(UntrustedCertificateException ex) {
      BddErrorMessage validationErrMsg = new BddErrorMessage("BDD.SECURITY.TLS_CERT_UNTRUSTED", "");
      validationErrMsg.setCertInfo(ex.getCertInfo());

      return validationErrMsg;
   }

   @ExceptionHandler(BddException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ResponseBody
   public BddErrorMessage processBddException(BddException ex) {
      return new BddErrorMessage(ex.getFullErrorId(), ex.getMessage());
   }

   @ExceptionHandler(Throwable.class)
   @ResponseBody
   public BddErrorMessage handleException(Throwable t,
         HttpServletResponse response) {
      if (t instanceof NestedRuntimeException) {
         t = BddException.BAD_REST_CALL(t, t.getMessage());
      }

      BddException ex = BddException.wrapIfNeeded(t, "Unknown error.");
      logger.error("Internal Server Error", ex);
      response.setStatus(getHttpErrorCode(ex.getFullErrorId()));
      response.setContentType("application/json;charset=utf-8");
      response.setCharacterEncoding("utf-8");
      BddErrorMessage msg = new BddErrorMessage(ex.getFullErrorId(), extractErrorMessage(ex));

      return msg;
   }

   private static final String ERR_CODE_FILE = "serengeti-errcode.properties";
   private static final int DEFAULT_HTTP_ERROR_CODE = 500;
   /* HTTP status code read from a config file. */
   private static org.apache.commons.configuration.Configuration httpStatusCodes =
         init();

   private static org.apache.commons.configuration.Configuration init() {
      PropertiesConfiguration config = null;
      try {
         config = new PropertiesConfiguration();
         config.setListDelimiter('\0');
         config.load(ERR_CODE_FILE);
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Cannot load Serengeti error message file.";
         Logger.getLogger(RestValidationErrorHandler.class).fatal(message, ex);
         throw BddException.APP_INIT_ERROR(ex, message);
      }
      return config;
   }

   private String extractErrorMessage(BddException ex) {
      String msg = ex.getMessage();
      if (ex.getCause() instanceof DataAccessException) {
         msg = "Data access layer exception. See the detailed error in the log";
      }
      return msg;
   }

   private static int getHttpErrorCode(String errorId) {
      return httpStatusCodes.getInteger(errorId, DEFAULT_HTTP_ERROR_CODE);
   }

   private ValidationErrors processFieldErrors(List<FieldError> fieldErrors) {
      ValidationErrors dto = new ValidationErrors();

      for (FieldError fieldError: fieldErrors) {
         dto.addError(fieldError.getField(), getError(fieldError));
      }

      return dto;
   }

   private ValidationError getError(FieldError fieldError) {
      ValidationError error = new ValidationError();

      error.setPrimaryCode(fieldError.getCodes()[0]);

      String localizedErrorMessage = messageSource.getMessage(fieldError, LocaleContextHolder.getLocale());
      error.setMessage(localizedErrorMessage);

      return error;
   }
}
