package com.vmware.bdd.apitypes;

/**
 * This class is a DTO of the cluster autoScale command.
 */
public class AutoScale {
   private Boolean defaultValue;
   private Boolean enable;
   private String clusterName;

   public Boolean getDefaultValue() {
      return defaultValue;
   }
   public void setDefaultValue(boolean defaultValue) {
      this.defaultValue = defaultValue;
   }
   public Boolean getEnable() {
      return enable;
   }
   public void setEnable(boolean enable) {
      this.enable = enable;
   }
   public String getClusterName() {
      return clusterName;
   }
   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }
}
