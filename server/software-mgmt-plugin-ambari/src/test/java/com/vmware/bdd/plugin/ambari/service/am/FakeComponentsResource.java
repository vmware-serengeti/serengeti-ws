package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.ComponentsResource;

public class FakeComponentsResource implements ComponentsResource {

   private String stackVersion;

   public FakeComponentsResource(String stackVersion) {
      this.stackVersion = stackVersion;
   }

   @Override
   public Response readComponents() {
      return BuildResponse.buildResponse("stacks/versions/" + stackVersion
            + "/stackServices/components/components.json");
   }

   @Override
   public Response readComponentsWithFilter(String fields) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readComponent(String componentName) {
      return BuildResponse.buildResponse("stacks/versions/" + stackVersion
            + "/stackServices/components/DATANODE_component.json");
   }

}
