/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.cli.rest;

import com.vmware.bdd.apitypes.AppManagerAdd;
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
public class AppManagerRestClient {

   @Autowired
   private RestClient restClient;

   public void add(AppManagerAdd pluginAdd) {
      final String path = Constants.REST_PATH_PLUGINS;
      final HttpMethod httpverb = HttpMethod.POST;
      restClient.createObject(pluginAdd, path, httpverb);
   }
}
