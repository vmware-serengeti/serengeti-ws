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
package com.vmware.bdd.validation;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Created By xiaoliangl on 11/27/14.
 */
public class ValidationError {
   private String primaryCode;
   private String message;

   public ValidationError(){}

   public ValidationError(String primaryCode, String message) {
      this.primaryCode = primaryCode;
      this.message = message;
   }

   public String getPrimaryCode() {
      return primaryCode;
   }

   public void setPrimaryCode(String primaryCode) {
      this.primaryCode = primaryCode;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }


   @Override
   public int hashCode() {
      return new HashCodeBuilder().append(primaryCode).append(message).hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if(obj == null || !(obj instanceof ValidationError)) return false;

      ValidationError another = (ValidationError) obj;

      return new EqualsBuilder().append(primaryCode, another.primaryCode).append(message, another.message).isEquals();
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this).append("primaryCode", primaryCode).append("message", message).toString();
   }
}
