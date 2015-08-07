package com.vmware.bdd.cli.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.VirtualMachineRead;
import com.vmware.bdd.cli.commands.Constants;

@Component
public class NodeTemplateRestClient {
   @Autowired
   private RestClient restClient;

   public VirtualMachineRead[] list() {
      final String path = Constants.REST_PATH_TEMPLATES;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(VirtualMachineRead[].class, path, httpverb, false);
   }
}
