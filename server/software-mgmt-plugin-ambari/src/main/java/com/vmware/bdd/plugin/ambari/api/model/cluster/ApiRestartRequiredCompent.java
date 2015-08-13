/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class ApiRestartRequiredCompent {

   private String name;

   private Set<String> hosts = new HashSet<String>();

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Set<String> getHosts() {
      return hosts;
   }

   public void setHosts(Set<String> hosts) {
      this.hosts = hosts;
   }

   public void addHost(String host) {
      this.hosts.add(host);
   }

   public String getStringHosts() {
      return StringUtils.join(this.hosts, ",");
   }
}
