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
package com.vmware.bdd.software.mgmt.plugin.impl;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

public class DefaultSoftwareManagerImpl implements SoftwareManager {

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
   public Set<String> getSupportedRoles() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint) throws Exception {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String queryClusterStatus(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return null;
   }

}
