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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
      if(ex instanceof UntrustedCertificateException) {
         return processUntrustedCertificate((UntrustedCertificateException)ex);
      }

      if(ex instanceof ValidationException) {
         return processValidationError((ValidationException)ex);
      }

      return new BddErrorMessage(ex.getFullErrorId(), ex.getMessage());
   }

   @ExceptionHandler(RuntimeException.class)
   @ResponseBody
   public BddErrorMessage processRuntimeException(RuntimeException ex, HttpServletResponse response) {
      if(ex instanceof BddException) {
         return processBddException((BddException) ex);
      }

      response.setStatus(500);
      BddException ex1 = BddException.BAD_REST_CALL(ex, "Internal Server Error");
      return new BddErrorMessage(ex1.getFullErrorId(), ex.getMessage());
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
