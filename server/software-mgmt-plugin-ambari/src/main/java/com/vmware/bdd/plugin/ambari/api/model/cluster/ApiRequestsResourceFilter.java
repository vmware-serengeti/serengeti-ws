package com.vmware.bdd.plugin.ambari.api.model.cluster;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by qjin on 12/6/14.
 */
public class ApiRequestsResourceFilter {
   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("component_name")
   private String componentName;

   public String getServiceName() {
      return serviceName;
   }

   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   public String getComponentName() {
      return componentName;
   }

   public void setComponentName(String componentName) {
      this.componentName = componentName;
   }
}
