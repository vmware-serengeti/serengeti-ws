package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.List;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

public class AbstractSoftwareManager implements SoftwareManager {

   public AbstractSoftwareManager() {
      // TODO Auto-generated constructor stub
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
   public String getVersion() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean echo() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean validateServerVersion() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public HealthStatus getStatus() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HadoopStack getDefaultStack() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint) throws ValidationException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames, ClusterReportQueue reports)
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
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void decommissionNode(ClusterBlueprint blueprint, String nodeGroupName, String nodeName,
         ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean recomissionNode(String clusterName, NodeInfo node, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<String> validateRolesForScaleOut(NodeGroupInfo group) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void validateRolesForShrink(NodeGroupInfo groupInfo) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub

   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) throws SoftwareManagementPluginException {
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
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean containsComputeOnlyNodeGroups(ClusterBlueprint blueprint) {
      for (NodeGroupInfo nodeGroup : blueprint.getNodeGroups()) {
         if (isComputeOnlyRoles(nodeGroup.getRoles())) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasMountPointStartwithDatax(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

}
