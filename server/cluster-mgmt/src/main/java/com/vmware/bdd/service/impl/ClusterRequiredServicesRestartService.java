/******************************************************************************
 *   Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
 *****************************************************************************/
package com.vmware.bdd.service.impl;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.IClusterRequiredServicesRestartService;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

public class ClusterRequiredServicesRestartService implements IClusterRequiredServicesRestartService {

   private static final Logger logger = Logger.getLogger(ClusterRequiredServicesRestartService.class);

   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;

   @Autowired
   private SoftwareManagementService softwareManagementService;

   @Override
   public void restart(ClusterBlueprint blueprint, ClusterReportQueue reports) {

      String clusterName = blueprint.getName();

      logger.info("Restarting required services of cluster " + clusterName + ".");

      SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);

      try {
         softwareManager.restartClusterRequiredServices(blueprint, reports);
      } catch (Exception e) {
         throw SoftwareManagementPluginException.RESTART_CLUSTER_SERVICE_FAILED(null, softwareManager.getName(), clusterName);
      }
   }

   public SoftwareManagerCollector getSoftwareManagerCollector() {
      return softwareManagerCollector;
   }

   public void setSoftwareManagerCollector(
         SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

   public SoftwareManagementService getSoftwareManagementService() {
      return softwareManagementService;
   }

   public void setSoftwareManagementService(
         SoftwareManagementService softwareManagementService) {
      this.softwareManagementService = softwareManagementService;
   }

}
