/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.cli.commands.Constants;

@Component
public class DistroRestClient {
   @Autowired
   private RestClient restClient;

   public DistroRead get(String id) {
      final String path = Constants.REST_PATH_DISTRO;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObject(id, DistroRead.class, path, httpverb, false);
   }

   public DistroRead get(String appManager, String id) {
      final String path = appManager + "/" + Constants.REST_PATH_DISTRO;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObject(id, DistroRead.class, path, httpverb, false);
   }

   public DistroRead[] getAll() {
      final String path = Constants.REST_PATH_DISTROS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(DistroRead[].class, path, httpverb, false);
   }

   public DistroRead[] getAll(String appManager) {
      final String path = appManager + "/" + Constants.REST_PATH_DISTROS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(DistroRead[].class, path, httpverb, false);
   }
}
