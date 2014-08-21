package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.Stacks2Resource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.VersionsResource;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.utils.CommonUtil;

public class FakeStacks2Resource implements Stacks2Resource {

   @Override
   public Response readStacks() {
      String stacks = CommonUtil.readJsonFile("simple_stacks.json");
      ResponseBuilder builder = Response.ok(stacks, "text/plain");
      return builder.build();
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
