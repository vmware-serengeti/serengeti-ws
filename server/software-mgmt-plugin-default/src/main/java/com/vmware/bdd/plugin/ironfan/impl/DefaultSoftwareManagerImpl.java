/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ironfan.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.IronfanStack;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;


public class DefaultSoftwareManagerImpl implements SoftwareManager {
   private static final Logger logger = Logger.getLogger(DefaultSoftwareManagerImpl.class);
   private ClusterValidator validator;
   private InfrastructureUpdator updator;
   private DistroManager distroManager;

   private static final String mapredConfigsFile = "mapred.json";
   private static final String yarnConfigsFile = "yarn.json";
   private String mapredConfigs = "";
   private String yarnConfigs = "";

   public DefaultSoftwareManagerImpl() {
      validator = new ClusterValidator();
      updator = new InfrastructureUpdator();
      updator.setValidator(validator);
      distroManager = new DistroManager();
      mapredConfigs = CommonUtil.readJsonFile(mapredConfigsFile);
      yarnConfigs = CommonUtil.readJsonFile(yarnConfigsFile);
   }

   @Override
   public String getName() {
      return Constants.IRONFAN;
   }

   @Override
   public String getDescription() {
      return "Default software management tool";
   }

   @Override
   public String getType() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean echo() {
      // TODO Auto-generated method stub
      return true;

   }

   @Override
   public HealthStatus getStatus() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() throws SoftwareManagementPluginException {
      List<HadoopStack> stacks = new ArrayList<HadoopStack>();
      List<DistroRead> distros = distroManager.getDistros();
      IronfanStack stack = null;
      String packagesExistStatus = "";
      Map <String, String> hadoopDistroMap = null;
      for (DistroRead distro : distros) {
         stack = new IronfanStack();
         stack.setDistro(distro.getName());
         stack.setVendor(distro.getVendor());
         stack.setFullVersion(distro.getVersion());
         stack.setHveSupported(distro.isHveSupported());
         stack.setRoles(distro.getRoles());
         packagesExistStatus = distroManager.checkPackagesExistStatus(distro.getName()).toString();
         stack.setPackagesExistStatus(packagesExistStatus);
         hadoopDistroMap = new HashMap<String, String>();
         hadoopDistroMap.put("HadoopUrl", distroManager.getPackageUrlByDistroRole(distro.getName(), HadoopRole.HADOOP_NAMENODE_ROLE.toString()));
         hadoopDistroMap.put("HiveUrl", distroManager.getPackageUrlByDistroRole(distro.getName(), HadoopRole.HIVE_ROLE.toString()));
         hadoopDistroMap.put("PigUrl", distroManager.getPackageUrlByDistroRole(distro.getName(), HadoopRole.PIG_ROLE.toString()));
         hadoopDistroMap.put("HbaseUrl", distroManager.getPackageUrlByDistroRole(distro.getName(), HadoopRole.HBASE_MASTER_ROLE.toString()));
         hadoopDistroMap.put("ZookeeperUrl", distroManager.getPackageUrlByDistroRole(distro.getName(), HadoopRole.ZOOKEEPER_ROLE.toString()));
         stack.setHadoopDistroMap(hadoopDistroMap);
         stack.setPackageRepos(distroManager.getPackageRepos(distro.getName()));
         stacks.add(stack);
      }
      return stacks;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) throws SoftwareManagementPluginException {
      if (Constants.MAPR_VENDOR.equalsIgnoreCase(stack.getVendor())) {
         return "Not supported";
      }
      List<String> roles = stack.getRoles();
      if (roles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString())
            || roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE)) {
         return yarnConfigs;
      } else {
         return mapredConfigs;
      }
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint)
         throws ValidationException {
      DistroRead distroRead =
            this.distroManager.getDistroByName(blueprint.getHadoopStack()
                  .getDistro());
      if (distroRead == null || distroRead.getRoles() == null) {
         return false;
      }
      return validator.validateBlueprint(blueprint, distroRead.getRoles());
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
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
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // We don't acctually stop any services for ironfan deployed cluster
      return true;
   }

   @Override
   public boolean onDeleteCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException {
      // do nothing
      return true;
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
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      // Ironfan does not monitor cluster status after cluster is created
      return null;
   }

   @Override
   public List<String> validateScaling(NodeGroupInfo group)
   throws SoftwareManagementPluginException {
      // resize of job tracker and name node is not supported
      List<String> roles = group.getRoles();
      List<String> unsupportedRoles = new ArrayList<String>();
      if (roles.isEmpty()) {
         // no unsupported roles
         return new ArrayList<String>();
      }
      if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.ZOOKEEPER_ROLE.toString());
      }
      return unsupportedRoles;
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) {
      updator.updateInfrastructure(blueprint);
   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      boolean hasHbase = false;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (HadoopRole.hasHBaseRole(group.getRoles())) {
            hasHbase = true;
            break;
         }
      }
      return hasHbase;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      return HadoopRole.hasMgmtRole(roles);
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      if (roles != null
            && ((roles.size() == 1 && roles.get(0).equals(
                  HadoopRole.HADOOP_TASKTRACKER.toString()))
                  || (roles.size() == 2
                        && roles.contains(HadoopRole.HADOOP_TASKTRACKER
                              .toString()) && roles
                              .contains(HadoopRole.TEMPFS_CLIENT_ROLE.toString())) || (roles
                                    .contains(HadoopRole.MAPR_TASKTRACKER_ROLE.toString()) && !roles
                                    .contains(HadoopRole.MAPR_NFS_ROLE.toString())))) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(group.getRoles(), new ArrayList<String>());
      if ((enumRoles.size() == 1 || (enumRoles.size() == 2 && enumRoles
            .contains(HadoopRole.HADOOP_JOURNALNODE_ROLE)))
            && (enumRoles.contains(HadoopRole.ZOOKEEPER_ROLE) || enumRoles
                  .contains(HadoopRole.MAPR_ZOOKEEPER_ROLE))) {
         return true;
      }
      return false;
   }

   @Override
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      List<String> roles = new ArrayList<String>();
      String distroVendor = blueprint.getHadoopStack().getVendor();
      if (nodeGroups != null) {
         for (NodeGroupInfo nodeGroup : nodeGroups) {
            roles.addAll(nodeGroup.getRoles());
         }
      }
      if (distroVendor.equalsIgnoreCase(Constants.MAPR_VENDOR)) {
         if (!roles.contains(HadoopRole.MAPR_JOBTRACKER_ROLE.toString())) {
            return false;
         }
      } else {
         if (!roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())
               && !roles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE
                     .toString())) {
            return false;
         }
      }
      return true;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager#getVersion()
    */
   @Override
   public String getVersion() {
      return Configuration.getNonEmptyString("serengeti.version");
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = getSupportedStacks();
      for (HadoopStack stack : hadoopStacks) {
         if (Constants.DEFAULT_VENDOR.equalsIgnoreCase(stack.getVendor())) {
            return stack;
         }
      }
      return null;
   }

}

