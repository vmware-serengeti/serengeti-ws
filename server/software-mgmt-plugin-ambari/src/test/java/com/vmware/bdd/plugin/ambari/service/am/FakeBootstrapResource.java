package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.BootstrapResource;

public class FakeBootstrapResource implements BootstrapResource {

   @Override
   public Response createBootstrap(String bootstrap) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readBootstrapStatus(Long bootstrapId) {
      // TODO Auto-generated method stub
      return null;
   }

}
