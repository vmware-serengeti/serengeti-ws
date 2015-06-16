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
package com.vmware.bdd.plugin.ambari.poller;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

public class ExternalNodesRegisterPoller extends StatusPoller {

   private static final Logger logger = Logger.getLogger(ExternalNodesRegisterPoller.class);

   private ApiManager apiManager;
   private List<String> externalHosts;
   private long startTime;
   private int maxWaitingSeconds = 60;
   private List<String> registerFailedHosts;

   public ExternalNodesRegisterPoller(final ApiManager apiManager, List<String> externalHosts) {
      this.apiManager = apiManager;
      this.externalHosts = externalHosts;
      this.startTime = System.currentTimeMillis();
   }

   @Override
   public boolean poll() {
      logger.info("Waiting for the external hosts " + externalHosts.toString() + " to register to Ambari server.");

      List<String> registeredHosts = new ArrayList<String>();

      if (isNotTimeout()) {
         ApiHostList apiHostList = apiManager.getRegisteredHosts();
         for (ApiHost apiHost : apiHostList.getApiHosts()) {
            registeredHosts.add(apiHost.getApiHostInfo().getHostName());
         }

         if (registeredHosts.containsAll(externalHosts)) {
            return true;
         } else {
            return false;
         }
      } else {
         registerFailedHosts = new ArrayList<String>();
         for (String externalHost : externalHosts) {
            if (!registeredHosts.contains(externalHost)) {
               registerFailedHosts.add(externalHost);
               logger.info("The external host " + externalHost + " register to Ambari server failed.");
            }
         }
         return true;
      }

   }

   private boolean isNotTimeout() {
      long timeout = this.maxWaitingSeconds * 1000;
      return System.currentTimeMillis() - startTime < timeout;
   }

   public List<String> getRegisterFailedHosts() {
      return registerFailedHosts;
   }

   public void setRegisterFailedHosts(List<String> registerFailedHosts) {
      this.registerFailedHosts = registerFailedHosts;
   }

}
