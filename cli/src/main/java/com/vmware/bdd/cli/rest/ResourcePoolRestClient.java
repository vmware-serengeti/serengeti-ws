/******************************************************************************
 *       Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *      Licensed under the Apache License, Version 2.0 (the "License");
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.ResourcePoolAdd;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.cli.commands.Constants;

@Component
public class ResourcePoolRestClient {
   @Autowired
   private RestClient restClient;

   public void add(ResourcePoolAdd rpAdd) {
      final String path = Constants.REST_PATH_RESOURCEPOOLS;
      final HttpMethod httpverb = HttpMethod.POST;

      restClient.createObject(rpAdd, path, httpverb);
   }

   public void delete(String id) {
      final String path = Constants.REST_PATH_RESOURCEPOOL;
      final HttpMethod httpverb = HttpMethod.DELETE;

      restClient.deleteObject(id, path, httpverb);
   }

   public ResourcePoolRead get(String id) {
      final String path = Constants.REST_PATH_RESOURCEPOOL;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObject(id, ResourcePoolRead.class, path, httpverb, false);
   }

   public ResourcePoolRead[] getAll() {
      final String path = Constants.REST_PATH_RESOURCEPOOLS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(ResourcePoolRead[].class, path, httpverb, false);
   }
}