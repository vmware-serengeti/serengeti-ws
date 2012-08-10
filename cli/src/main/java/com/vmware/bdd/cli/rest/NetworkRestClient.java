/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.cli.rest;

/**
 * This class is the realization of Network command.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.cli.commands.Constants;

@Component
public class NetworkRestClient {

   @Autowired
   private RestClient restClient;

   /**
    * Add function of network command.
    * 
    * @param networkAdd
    */
   public void add(NetworkAdd networkAdd) {
      final String path = Constants.REST_PATH_NETWORKS;
      final HttpMethod httpverb = HttpMethod.POST;
      restClient.createObject(networkAdd, path, httpverb);
   }

   /**
    * Delete function of network command.
    * 
    * @param name
    */
   public void delete(String name) {
      final String path = Constants.REST_PATH_NETWORK;
      final HttpMethod httpverb = HttpMethod.DELETE;
      restClient.deleteObject(name, path, httpverb);
   }
   /**
    * Find all network information.
    * @return  network list
    */
   public NetworkRead[] getAll(boolean detail) {
      final HttpMethod httpverb = HttpMethod.GET;
      String path = Constants.REST_PATH_NETWORKS;

      return restClient.getAllObjects(NetworkRead[].class, path, httpverb, detail);
   }
   /**
    * Find a network information by name.
    * @param network's name
    * @return a network information
    */
   public NetworkRead get(String name, boolean detail) {
      final String path = Constants.REST_PATH_NETWORK;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObject(name, NetworkRead.class, path, httpverb, detail);
   }
}
