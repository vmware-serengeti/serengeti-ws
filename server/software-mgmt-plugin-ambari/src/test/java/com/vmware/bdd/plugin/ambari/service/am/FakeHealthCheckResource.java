package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.vmware.bdd.plugin.ambari.api.v1.HealthCheckResource;
import com.vmware.bdd.plugin.ambari.utils.Constants;

public class FakeHealthCheckResource implements HealthCheckResource {

   @Override
   public Response check() {
      ResponseBuilder builder = Response.ok(Constants.HEALTH_STATUS, "text/plain");
      return builder.build();
   }

}
