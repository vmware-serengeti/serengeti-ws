/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job;

import java.util.Map;

import com.vmware.bdd.utils.JobUtils;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.IClusterOperationCallbackService;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.Version;

public class ClusterOperationCallbackStep extends TrackableTasklet {

   private ClusterManager clusterManager;
   private SoftwareManagerCollector softwareMgrs;
   private ManagementOperation managementOperation;
   private IClusterOperationCallbackService clusterOperationCallbackService;
   private String phase;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);

      SoftwareManager softwareMgr = softwareMgrs.getSoftwareManagerByClusterName(clusterName);
      String appMgrType = softwareMgr.getType();

      ClusterBlueprint clusterBlueprint = clusterManager.getClusterEntityMgr().toClusterBluePrint(clusterName);
      HadoopStack hadoopStack = clusterBlueprint.getHadoopStack();
      String vendorName = hadoopStack.getVendor();
      String distroVersion = hadoopStack.getFullVersion();
      String appMgrVersion = softwareMgr.getVersion();

      // This is a patch for Ambari version < 2.1  only. Ambari Blueprint API doesn't support configuring Rack Topology.
      if (appMgrType.equalsIgnoreCase(Constants.AMBARI_PLUGIN_TYPE) && Version.compare(appMgrVersion, "2.1") < 0) {
         if (clusterBlueprint.hasTopologyPolicy()) {
            Map<String, String> rackTopology = this.clusterManager.getRackTopology(clusterName, null);
            String filename = Constants.CLUSTER_RACK_TOPOLOGY_FILE_PATH + clusterName
                  + Constants.CLUSTER_RACK_TOPOLOGY_FILE_SUFFIX;
            String msg = "Generating rack topology data file " + filename + " for cluster " + clusterName;
            logger.info(msg);
            try {
               CommonUtil.gracefulRackTopologyOutput(rackTopology, filename, System.lineSeparator());
            } catch (Exception e) {
               String errorMessage = msg + " failed. " + e.getLocalizedMessage();
               boolean force = JobUtils.getJobParameterForceClusterOperation(chunkContext);
               if (!force || (managementOperation == ManagementOperation.RESIZE)) {
                  throw TaskException.EXECUTION_FAILED(errorMessage);
               }
            }
         }
      }

      clusterOperationCallbackService.invoke(phase, clusterName, managementOperation.toString(), appMgrType, appMgrVersion, vendorName, distroVersion);

      return RepeatStatus.FINISHED;
   }

   public ClusterManager getClusterManager() {
      return clusterManager;
   }

   public void setClusterManager(ClusterManager clusterManager) {
      this.clusterManager = clusterManager;
   }

   public SoftwareManagerCollector getSoftwareMgrs() {
      return softwareMgrs;
   }

   @Autowired
   public void setSoftwareMgrs(SoftwareManagerCollector softwareMgrs) {
      this.softwareMgrs = softwareMgrs;
   }

   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }

   public IClusterOperationCallbackService getClusterOperationCallbackService() {
      return clusterOperationCallbackService;
   }

   public void setClusterOperationCallbackService(
         IClusterOperationCallbackService clusterOperationCallbackService) {
      this.clusterOperationCallbackService = clusterOperationCallbackService;
   }

   public String getPhase() {
      return phase;
   }

   public void setPhase(String phase) {
      this.phase = phase;
   }

}
