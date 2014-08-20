package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.Stacks2Resource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.VersionsResource;

public class FakeStacks2Resource implements Stacks2Resource {

   @Override
   public Response readStacks() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readStack(String stackName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public VersionsResource getStackVersionsResource(String stackName) {
      // TODO Auto-generated method stub
      return null;
   }

}
