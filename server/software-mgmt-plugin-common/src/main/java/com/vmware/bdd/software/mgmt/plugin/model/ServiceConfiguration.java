package com.vmware.bdd.software.mgmt.plugin.model;

/**
 * Hadoop configuration information
 * @author line
 *
 */
public class ServiceConfiguration {
   public enum ValidationState{
      OK,
      WARNING,
      ERROR
   }
   private String name;
   private String value;
   private Boolean required;
   private String defaultValue;
   private String description;
   private ValidationState validationState;
   private String validationMessage;

   public ServiceConfiguration() {
   }

   public ServiceConfiguration(String name, String value) {
      this.name = name;
      this.value = value;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Boolean getRequired() {
      return required;
   }

   public void setRequired(Boolean required) {
      this.required = required;
   }

   public String getDefaultValue() {
      return defaultValue;
   }

   public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public ValidationState getValidationState() {
      return validationState;
   }

   public void setValidationState(ValidationState validationState) {
      this.validationState = validationState;
   }

   public String getValidationMessage() {
      return validationMessage;
   }

   public void setValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
   }
}
