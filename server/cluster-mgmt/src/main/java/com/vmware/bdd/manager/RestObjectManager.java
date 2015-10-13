/***************************************************************************
 * Copyright (c) 2015-2015 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.PlacementPolicy;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupRacks;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.InfrastructureConfigUtils;

public class RestObjectManager {

   @SuppressWarnings("unchecked")
   public static ClusterRead clusterEntityToRead(ClusterEntity cluster) {
      // Following fields of ClusterRead object are not set here, need to be set somewhere else
      // if consumer of the ClusterRead object need them:
      // - instanceNum
      // - nodeGroup
      // - templateName
      // - ResourcePool
      // - computeOnly

      ClusterRead clusterRead = new ClusterRead();

      ClusterStatus clusterStatus = cluster.getStatus();
      clusterRead.setName(cluster.getName());
      clusterRead.setStatus(clusterStatus);
      clusterRead.setAppManager(cluster.getAppManager());
      clusterRead.setDistro(cluster.getDistro());
      clusterRead.setDistroVendor(cluster.getDistroVendor());
      clusterRead.setTopologyPolicy(cluster.getTopologyPolicy());
      clusterRead.setAutomationEnable(cluster.getAutomationEnable());
      clusterRead.setVhmMinNum(cluster.getVhmMinNum());
      clusterRead.setVhmMaxNum(cluster.getVhmMaxNum());
      clusterRead.setVhmTargetNum(cluster.getVhmTargetNum());
      clusterRead.setIoShares(cluster.getIoShares());
      clusterRead.setVersion(cluster.getVersion());
      if (!CommonUtil.isBlank(cluster.getAdvancedProperties())) {
         Gson gson = new Gson();
         Map<String, String> advancedProperties =
               gson.fromJson(cluster.getAdvancedProperties(), Map.class);
         clusterRead.setExternalHDFS(advancedProperties.get("ExternalHDFS"));
         clusterRead.setExternalMapReduce(advancedProperties
               .get("ExternalMapReduce"));
         clusterRead.setLocalRepoURL(advancedProperties.get("LocalRepoURL"));
         clusterRead.setClusterCloneType(advancedProperties.get("ClusterCloneType"));
         clusterRead.setExternalNamenode(advancedProperties.get("ExternalNamenode"));
         clusterRead.setExternalSecondaryNamenode(advancedProperties.get("ExternalSecondaryNamenode"));
         if (advancedProperties.get("ExternalDatanodes") != null) {
            clusterRead.setExternalDatanodes(gson.fromJson(gson.toJson(advancedProperties.get("ExternalDatanodes")), HashSet.class));
         }
      }

      String cloneType = clusterRead.getClusterCloneType();
      if (CommonUtil.isBlank(cloneType)) {
         // for clusters from previous releases, it should be fast clone
         clusterRead.setClusterCloneType(Constants.CLUSTER_CLONE_TYPE_FAST_CLONE);
      }

      if (clusterStatus.isActiveServiceStatus()
            || clusterStatus == ClusterStatus.STOPPED) {
         clusterRead.setDcSeperation(clusterRead.validateSetManualElasticity());
      }

      if(StringUtils.isNotBlank(cluster.getInfraConfig())) {
         clusterRead.setInfrastructure_config(InfrastructureConfigUtils.read(cluster.getInfraConfig()));
      }

      return clusterRead;
   }

   public static NodeGroupRead nodeGroupEntityToRead(NodeGroupEntity ng) {
      NodeGroupRead nodeGroupRead = new NodeGroupRead();
      nodeGroupRead.setName(ng.getName());
      nodeGroupRead.setCpuNum(ng.getCpuNum());
      nodeGroupRead.setMemCapacityMB(ng.getMemorySize());
      nodeGroupRead.setSwapRatio(ng.getSwapRatio());

      Gson gson = new Gson();
      @SuppressWarnings("unchecked")
      List<String> groupRoles = gson.fromJson(ng.getRoles(), List.class);
      nodeGroupRead.setRoles(groupRoles);

      StorageRead storage = new StorageRead();
      storage.setType(ng.getStorageType().toString());
      storage.setSizeGB(ng.getStorageSize());
      storage.setDiskNum(ng.getDiskNum());
      storage.setShareDatastore(ng.isShareDatastore());

      // set dsNames/dsNames4Data/dsNames4System
      List<String> datastoreNameList = ng.getVcDatastoreNameList();
      if (datastoreNameList != null && !datastoreNameList.isEmpty())
         storage.setDsNames(datastoreNameList);
      if (ng.getSdDatastoreNameList() != null
            && !ng.getSdDatastoreNameList().isEmpty())
         storage.setDsNames4System(ng.getSdDatastoreNameList());
      if (ng.getDdDatastoreNameList() != null
            && !ng.getDdDatastoreNameList().isEmpty())
         storage.setDsNames4Data(ng.getDdDatastoreNameList());

      nodeGroupRead.setStorage(storage);

      List<GroupAssociation> associations = new ArrayList<GroupAssociation>();
      for (NodeGroupAssociation relation : ng.getGroupAssociations()) {
         GroupAssociation association = new GroupAssociation();
         association.setReference(relation.getReferencedGroup());
         association.setType(relation.getAssociationType());
         associations.add(association);
      }

      PlacementPolicy policy = new PlacementPolicy();
      policy.setInstancePerHost(ng.getInstancePerHost());
      policy.setGroupAssociations(associations);
      policy.setGroupRacks(new Gson().fromJson(ng.getGroupRacks(), GroupRacks.class));

      nodeGroupRead.setPlacementPolicies(policy);

      return nodeGroupRead;
   }

   public static NodeRead nodeEntityToRead(NodeEntity node, boolean includeVolumes) {
      NodeRead nodeRead = new NodeRead();
      nodeRead.setRack(node.getRack());
      nodeRead.setHostName(node.getHostName());
      // For class NodeRead, keep "ipConfigsInfo" structure since it's used by software provision
      nodeRead.setIpConfigs(node.convertToIpConfigInfo());
      nodeRead.setName(node.getVmName());
      nodeRead.setMoId(node.getMoId());
      nodeRead.setStatus(node.getStatus() != null ? node.getStatus().toString() : null);
      nodeRead.setAction(node.getAction());
      nodeRead.setVersion(node.getVersion());
      if (node.getCpuNum() != null) {
         nodeRead.setCpuNumber(node.getCpuNum());
      }
      if (node.getMemorySize() != null) {
         nodeRead.setMemory(node.getMemorySize());
      }
      List<String> roleNames = node.getNodeGroup().getRoleNameList();
      nodeRead.setRoles(roleNames);
      if (includeVolumes) {
         nodeRead.setVolumes(node.getVolumns());
      }
      if (node.isActionFailed()) {
         nodeRead.setActionFailed(true);
      }
      if (node.getErrMessage() != null && !node.getErrMessage().isEmpty()) {
         nodeRead.setErrMessage(node.getErrMessage());
      }
      return nodeRead;
   }

   public static ResourcePoolRead vcResourcePoolEntityToRead(VcResourcePoolEntity rp) {
      ResourcePoolRead rpRead = new ResourcePoolRead();
      rpRead.setRpName(rp.getName());
      rpRead.setRpVsphereName(rp.getVcResourcePool());
      rpRead.setVcCluster(rp.getVcCluster());

      return rpRead;
   }
}
