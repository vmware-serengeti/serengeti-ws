/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.exception;

import java.util.Map;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 12/2/14.
 */
public class ValidationErrorsMessage extends BddErrorMessage {
   private Map<String, ValidationError> errors;

   public Map<String, ValidationError> getErrors() {
      return errors;
   }

   public void setErrors(Map<String, ValidationError> errors) {
      this.errors = errors;
   }

   public ValidationErrorsMessage(String code, String message, Map<String, ValidationError> validationErrors) {
      super(code, message);

      this.errors = validationErrors;
   }
}
