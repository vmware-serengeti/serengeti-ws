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
package com.vmware.bdd.model.support;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 3:44 PM
 */
public class AvailableConfiguration {

   @Expose
   private String name;

   @Expose
   private boolean required;

   @Expose
   @SerializedName("default")
   private String defaultValue;

   @Expose
   private String displayName;

   @Expose
   private String description;

   @Expose
   private String relatedName;

   @Expose
   private String validationState; // TODO: remove since it maybe of no use

   @Expose
   private String validationMessage; // TODO: remove

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean isRequired() {
      return required;
   }

   public void setRequired(boolean required) {
      this.required = required;
   }

   public String getDefaultValue() {
      return defaultValue;
   }

   public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
   }

   public void setDefault(String defaultValue) {
      setDefaultValue(defaultValue);
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getRelatedName() {
      return relatedName;
   }

   public void setRelatedName(String relatedName) {
      this.relatedName = relatedName;
   }

   public String getValidationState() {
      return validationState;
   }

   public void setValidationState(String validationState) {
      this.validationState = validationState;
   }

   public String getValidationMessage() {
      return validationMessage;
   }

   public void setValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
   }

}
