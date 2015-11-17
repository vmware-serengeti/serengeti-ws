/******************************************************************************
 *   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.rest.advice;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.vmware.bdd.exception.WarningMessageException;

import com.vmware.bdd.utils.ServerErrorMessages;
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
 * Handling Spring-Validation Exceptions at REST level
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

      BddException validationException = new ValidationException(processFieldErrors(fieldErrors).getErrors());
      return ServerErrorMessages.fromException(validationException);
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
