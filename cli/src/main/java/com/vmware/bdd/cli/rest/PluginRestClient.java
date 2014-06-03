package com.vmware.bdd.cli.rest;

import com.vmware.bdd.apitypes.PluginAdd;
import com.vmware.bdd.cli.commands.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:21 PM
 */
@Component
public class PluginRestClient {

   @Autowired
   private RestClient restClient;

   public void add(PluginAdd pluginAdd) {
      final String path = Constants.REST_PATH_PLUGINS;
      final HttpMethod httpverb = HttpMethod.POST;
      restClient.createObject(pluginAdd, path, httpverb);
   }
}
