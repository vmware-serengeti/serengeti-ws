/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.job.service.impl;

import com.google.gson.reflect.TypeToken;
import com.vmware.aurora.composition.DiskSchema;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.*;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.service.IClusterUpdateDataService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.spectypes.NicSpec;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.vmware.bdd.service.job.TrackableTasklet.*;

/**
 * Stripped from CLusterUpdateDataStep
 * Created by xiaoliangl on 10/30/15.
 */
@Component
public class ClusterUpdateDataServiceImpl implements IClusterUpdateDataService {
   private static final Logger logger = Logger.getLogger(ClusterUpdateDataServiceImpl.class);
   @Autowired
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   @Autowired
   private INetworkService networkMgr;
   @Autowired
   private IResourcePoolDAO rpDao;

   @Autowired
   private IClusterEntityManager clusterEntityMgr;


   /**
    * strip the logic here and use "REQUIRES_NEW" to ensure the nodes are persisted to DB here,
    * before invoking lockClusterEntityMgr.syncUp(clusterName, false)
    * If not, there will be null pointer exception during syncUp() and causes some postgres connection leaks.
    * the leaks will cause dead locks.
    * @param chunkContext job execution context
    */
   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void updateAndValidateNodes(ChunkContext chunkContext) {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      List<BaseNode> addedNodes =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM,
                  new TypeToken<List<BaseNode>>() {
                  }.getType());
      List<BaseNode> deletedNodes =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_DELETED_NODES_JOB_PARAM,
                  new TypeToken<List<BaseNode>>() {
                  }.getType());
      Set<String> deletedNodeNames = new HashSet<>();
      if (deletedNodes != null) {
         for (BaseNode node : deletedNodes) {
            deletedNodeNames.add(node.getVmName());
         }
      }
      lockClusterEntityMgr.getLock(clusterName).lock();
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_EXCLUSIVE_WRITE_LOCKED, true);

      addNodeToMetaData(addedNodes, deletedNodeNames);
      removeDeletedNode(deletedNodes, deletedNodeNames);

      /*
       * Verify node status and update error message
       * As we need to update db value, we cannot keep this verification in step will throw exception
       */
      verifyCreatedNodes(chunkContext, clusterName);
   }


   private void verifyCreatedNodes(ChunkContext chunkContext, String clusterName) {
      Boolean created =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS,
                  Boolean.class);
      String verifyScope =
            getJobParameters(chunkContext).getString(
                  JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM);
      String groupName = null;
      long oldInstanceNum = 0;
      if (verifyScope != null
            && verifyScope.equals(JobConstants.GROUP_NODE_SCOPE_VALUE)) {
         groupName =
               getJobParameters(chunkContext).getString(
                     JobConstants.GROUP_NAME_JOB_PARAM);
         oldInstanceNum =
               getJobParameters(chunkContext).getLong(
                     JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);
      }
      if (created != null && created) {
         boolean success =
               JobUtils.VerifyClusterNodes(clusterName, verifyScope, groupName,
                     oldInstanceNum, clusterEntityMgr);
         putIntoJobExecutionContext(chunkContext,
               JobConstants.VERIFY_NODE_STATUS_RESULT_PARAM, success);
      }
   }

   /**
    * Add successfully created node, which information is got from vc creation.
    * If deleted any VM, or nodes during vm creation step, which may violate
    * placement policy. We'll remove the node if it's not re-created.
    *
    * @param addedNodes
    * @param deletedNodeNames
    * @return
    */
   public void addNodeToMetaData(List<BaseNode> addedNodes,
                                 Set<String> deletedNodeNames) {
      if (addedNodes == null || addedNodes.isEmpty()) {
         logger.info("No node is added!");
         return;
      }

      for (BaseNode vNode : addedNodes) {
         deletedNodeNames.remove(vNode.getVmName());
         replaceNodeEntity(vNode);
      }
   }

   private void removeDeletedNode(final List<BaseNode> deletedNodes, Set<String> deletedNodeNames) {
      if (deletedNodeNames == null || deletedNodeNames.isEmpty()) {
         return;
      }

      for (BaseNode deletedNode : deletedNodes) {
         if (!deletedNodeNames.contains(deletedNode.getVmName())) {
            // do not touch already replaced VMs
            continue;
         }
         NodeEntity node = clusterEntityMgr.getNodeByVmName(deletedNode.getVmName());
         if (node != null) {
            if (deletedNode.isSuccess()) {
               clusterEntityMgr.delete(node);
            } else {
               node.setActionFailed(true);
               node.setErrMessage(deletedNode.getErrMessage());
            }
         }
      }
   }

   private void replaceNodeEntity(BaseNode vNode) {
      logger.info("Add or replace node info for VM " + vNode.getVmName());
      ClusterEntity cluster =
            clusterEntityMgr.findByName(vNode.getClusterName());
      AuAssert.check(cluster != null);
      NodeGroupEntity nodeGroupEntity =
            clusterEntityMgr.findByName(vNode.getClusterName(),
                  vNode.getGroupName());
      AuAssert.check(nodeGroupEntity != null);
      if (nodeGroupEntity.getNodes() == null) {
         nodeGroupEntity.setNodes(new HashSet<NodeEntity>());
      }

      boolean insert = false;
      NodeEntity nodeEntity =
            clusterEntityMgr.findByName(nodeGroupEntity, vNode.getVmName());
      // if node already exists, replace the value with new one from vNode.
      if (nodeEntity == null) {
         nodeEntity = new NodeEntity();
         nodeGroupEntity.getNodes().add(nodeEntity);
         insert = true;
      }
      nodeEntity.setVmName(vNode.getVmName());
      setNodeStatus(nodeEntity, vNode);
      if (vNode.getVmMobId() == null && nodeEntity.getMoId() != null) {
         vNode.setVmMobId(nodeEntity.getMoId());
      }

      // set node version
      nodeEntity.setVersion(cluster.getVersion());

      //set vc resource pool entity
      nodeEntity.setVcRp(rpDao.findByClusterAndRp(vNode.getTargetVcCluster(),
            vNode.getTargetRp()));

      // set ipconfigs field even IPs are not yet retrieved, otherwise if
      // Tomcat crashes, we will lost the ipconfigs template
      for (NicSpec nicSpec : vNode.getNics().values()) {
         NetworkEntity networkEntity =
               networkMgr.getNetworkEntityByName(nicSpec.getNetworkName());
         NicEntity nicEntity = nodeEntity.findNic(networkEntity);
         if (nicEntity == null) {
            nicEntity = new NicEntity();
            nodeEntity.getNics().add(nicEntity);
         }
         nicEntity.setIpv4Address(nicSpec.getIpv4Address());
         nicEntity.setIpv6Address(nicSpec.getIpv6Address());
         nicEntity.setMacAddress(nicSpec.getMacAddress());
         nicEntity.setNetTrafficDefs(nicSpec.getNetTrafficDefinitionSet());
         nicEntity.setNetworkEntity(networkEntity);
         nicEntity.setNodeEntity(nodeEntity);
         if (vNode.getVmMobId() != null) {
            VcVmUtil.populateNicInfo(nicEntity, vNode.getVmMobId(),
                  networkEntity.getPortGroup());
         }
      }

      if (vNode.isFinished()) {
         nodeEntity.setActionFailed(!vNode.isSuccess());
         nodeEntity.setErrMessage(vNode.getErrMessage());
      }

      if (vNode.getVmMobId() != null) {
         nodeEntity.setMoId(vNode.getVmMobId());
         nodeEntity.setRack(vNode.getTargetRack());
         nodeEntity.setHostName(vNode.getTargetHost());
         nodeEntity.setGuestHostName(vNode.getGuestHostName());
         nodeEntity.setCpuNum(vNode.getCpu());
         nodeEntity.setMemorySize((long) vNode.getMem());

         // set disk entities, include system/swap/data disk
         Set<DiskEntity> diskEntities = nodeEntity.getDisks();

         // system disk
         DiskEntity systemDisk = nodeEntity.findSystemDisk();
         if (systemDisk == null)
            systemDisk = new DiskEntity(nodeEntity.getVmName() + ".vmdk");
         systemDisk.setDiskType(StorageRead.DiskType.SYSTEM_DISK.getType());
         systemDisk.setExternalAddress(DiskEntity
               .getSystemDiskExternalAddress());
         systemDisk.setNodeEntity(nodeEntity);
         systemDisk.setDatastoreName(vNode.getTargetDs());
         VcVmUtil.populateDiskInfo(systemDisk, vNode.getVmMobId());
         diskEntities.add(systemDisk);

         // swap and data disk
         for (DiskSchema.Disk disk : vNode.getVmSchema().diskSchema.getDisks()) {
            DiskEntity newDisk = nodeEntity.findDisk(disk.name);
            if (newDisk == null) {
               newDisk = new DiskEntity(disk.name);
               diskEntities.add(newDisk);
            }
            newDisk.setSizeInMB(disk.initialSizeMB);
            newDisk.setAllocType(disk.allocationType.toString());
            newDisk.setDatastoreName(disk.datastore);
            newDisk.setDiskType(disk.type);
            newDisk.setExternalAddress(disk.externalAddress);
            newDisk.setNodeEntity(nodeEntity);

            // get vm object and find the vmdk path
            VcVmUtil.populateDiskInfo(newDisk, vNode.getVmMobId());
         }
      }
      nodeEntity.setNodeGroup(nodeGroupEntity);

      if (insert) {
         clusterEntityMgr.insert(nodeEntity);
      } else {
         clusterEntityMgr.update(nodeEntity);
      }

      logger.info("Finished node info replacement for VM " + vNode.getVmName());
   }

   private void setNodeStatus(NodeEntity nodeEntity, BaseNode vNode) {
      if (vNode.isFinished() && vNode.isSuccess()) {
         nodeEntity.setStatus(NodeStatus.VM_READY);
         nodeEntity.setAction(null);
         return;
      }
      nodeEntity.setAction(vNode.getNodeAction());
      if (vNode.getNodeStatus() == null) {
         nodeEntity.setStatus(NodeStatus.NOT_EXIST);
      } else {
         nodeEntity.setStatus(vNode.getNodeStatus());
      }
   }
}
