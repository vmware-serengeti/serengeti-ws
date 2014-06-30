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
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.utils.CommonUtil;

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

   public void add(AppManagerAdd appManagerAdd) {
      final String path = Constants.REST_PATH_APPMANAGERS;
      final HttpMethod httpverb = HttpMethod.POST;
      restClient.createObject(appManagerAdd, path, httpverb);
   }

   public AppManagerRead get(String name) {
      name = CommonUtil.encode(name);
      final String path = Constants.REST_PATH_APPMANAGER;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObject(name, AppManagerRead.class, path, httpverb,
            false);
   }

   public AppManagerRead[] getAll() {
      final String path = Constants.REST_PATH_APPMANAGERS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(AppManagerRead[].class, path, httpverb,
            false);
   }

   public HadoopStack[] getStacks(String name) {
      final String path =
            Constants.REST_PATH_APPMANAGER + "/" + name + "/"
                  + Constants.REST_PATH_STACKS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(HadoopStack[].class, path, httpverb,
            false);
   }
}
