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
package com.vmware.bdd.service.job.software;

import java.util.List;

import com.vmware.bdd.software.mgmt.plugin.intf.AbstractSoftwareManager;
import mockit.Mock;
import mockit.MockClass;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

@MockClass(realClass = SoftwareManager.class)
public class MockSoftwareManager extends AbstractSoftwareManager implements SoftwareManager {
   @Mock
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      ClusterReport report = new ClusterReport();
      report.setName(blueprint.getName());
      report.setAction("testing 1");
      report.setFinished(false);
      report.setProgress(30);
      reports.addClusterReport(report.clone());
      try {
         Thread.sleep(5000);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }

      report.setAction("testing 2");
      report.setProgress(60);
      reports.addClusterReport(report.clone());
      try {
         Thread.sleep(15000);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
      report.setAction("testing 3");
      report.setProgress(90);
      reports.addClusterReport(report.clone());
      try {
         Thread.sleep(3000);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
      report.setAction("testing finished");
      report.setProgress(100);
      report.setFinished(true);
      reports.addClusterReport(report.clone());
      return true;
   }

   @Override
   public String getName() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getDescription() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getType() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean echo() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean validateServerVersion() {
      return true;
   }

   @Override
   public HealthStatus getStatus() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks()
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports, boolean froce)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports, boolean forceStart)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void decommissionNode(ClusterBlueprint blueprint, String nodeGroupName, String nodeName, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
   }

   @Override
   public boolean recomissionNode(String clusterName, NodeInfo node, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<String> validateRolesForScaleOut(NodeGroupInfo group)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void validateRolesForShrink(NodeGroupInfo groupInfo) throws SoftwareManagementPluginException {

   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasMountPointStartwithDatax(String clusterName) {
      return false;
   }

   @Override
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager#getVersion()
    */
   @Override
   public String getVersion() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint)
         throws ValidationException {
      // TODO Auto-generated method stub
      return false;
   }
}
