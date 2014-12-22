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
package com.vmware.bdd.service.collection.impl;

import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.IResourceService;

public class FakePeriodCollectionService extends PeriodCollectionService {

   public IResourceInitializerService getResourceInitializerService() {
      return resourceInitializerService;
   }
   public void setResourceInitializerService(
         IResourceInitializerService resourceInitializerService) {
      this.resourceInitializerService = resourceInitializerService;
   }
   public ClusterManager getClusterMgr() {
      return clusterMgr;
   }
   public void setClusterMgr(ClusterManager clusterMgr) {
      this.clusterMgr = clusterMgr;
   }
   public IResourceService getResourceService() {
      return resourceService;
   }
   public void setResourceService(IResourceService resourceService) {
      this.resourceService = resourceService;
   }
   public IResourcePoolService getResourcePoolService() {
      return resourcePoolService;
   }
   public void setResourcePoolService(IResourcePoolService resourcePoolService) {
      this.resourcePoolService = resourcePoolService;
   }
   public SoftwareManagerCollector getSoftwareManagerCollector() {
      return softwareManagerCollector;
   }
   public void setSoftwareManagerCollector(
         SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

}
