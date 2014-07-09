package com.vmware.bdd.service.job.software;

import java.util.List;
import java.util.Set;

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
public class MockSoftwareManager implements SoftwareManager {
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
   public HealthStatus getStatus() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<String> getSupportedRoles(HadoopStack hadoopStack)
         throws SoftwareManagementPluginException {
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
   public boolean validateBlueprint(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
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
   public List<String> validateScaling(NodeGroupInfo group)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
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
}
