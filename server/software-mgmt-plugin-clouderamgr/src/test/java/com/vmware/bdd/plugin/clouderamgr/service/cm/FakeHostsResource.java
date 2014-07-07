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
package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostList;
import com.cloudera.api.model.ApiMetricList;
import com.cloudera.api.v2.HostsResourceV2;

import javax.ws.rs.DefaultValue;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 10:56 AM
 */
public class FakeHostsResource implements HostsResourceV2 {

   ApiHostList hosts;

   public FakeHostsResource() {
      hosts = new ApiHostList();
   }

   @Override
   public ApiCommand enterMaintenanceMode(String s) {
      return null;
   }

   @Override
   public ApiCommand exitMaintenanceMode(String s) {
      return null;
   }

   @Override
   public ApiHostList createHosts(ApiHostList apiHosts) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      for (ApiHost host : apiHosts) {
         hosts.add(host);
      }
      return hosts;
   }

   @Override
   public ApiHostList readHosts(@DefaultValue("summary") DataView dataView) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return hosts;
   }

   @Override
   public ApiHost readHost(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      for (ApiHost host : hosts) {
         if (host.getHostId().equals(s)) {
            return host;
         }
      }
      return null;
   }

   @Override
   public ApiHost updateHost(String s, ApiHost apiHost) {
      return null;
   }

   @Override
   public ApiHost deleteHost(String s) {
      return null;
   }

   @Override
   public ApiHostList deleteAllHosts() {
      return null;
   }

   @Override
   public ApiConfigList readHostConfig(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiConfigList updateHostConfig(String s, String s2, ApiConfigList apiConfigs) {
      return null;
   }

   @Override
   public ApiMetricList getMetrics(String s, String s2, @DefaultValue("now") String s3, @DefaultValue("true") boolean b,
         Set<String> strings, @DefaultValue("true") boolean b2, Set<String> strings2, Set<String> strings3,
         @DefaultValue("summary") DataView dataView) {
      return null;
   }
}
