/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AuAssert;

public class ClusterUpdateDataStep extends TrackableTasklet {

   private static final Logger logger = Logger
         .getLogger(ClusterUpdateDataStep.class);

   private INetworkService networkMgr;

   private IResourcePoolDAO rpDao;

   public INetworkService getNetworkMgr() {
      return networkMgr;
   }

   @Autowired
   public void setNetworkMgr(INetworkService networkMgr) {
      this.networkMgr = networkMgr;
   }

   /**
    * @return the rpDao
    */
   public IResourcePoolDAO getRpDao() {
      return rpDao;
   }

   /**
    * @param rpDao
    *           the rpDao to set
    */
   @Autowired
   public void setRpDao(IResourcePoolDAO rpDao) {
      this.rpDao = rpDao;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

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
      Set<String> deletedNodeNames = new HashSet<String>();
      if (deletedNodes != null) {
         for (BaseNode node : deletedNodes) {
            deletedNodeNames.add(node.getVmName());
         }
      }
      addNodeToMetaData(clusterName, addedNodes, deletedNodeNames);
      removeDeletedNode(clusterName, deletedNodeNames);
      Boolean deleted =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_DELETE_VM_OPERATION_SUCCESS,
                  Boolean.class);
      Boolean created =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS,
                  Boolean.class);
      if (deleted != null && !deleted) {
         logger.error("Failed to delete nodes violating placement policy.");
         throw ClusteringServiceException.DELETE_CLUSTER_VM_FAILED(clusterName);
      }
      if (created != null && !created) {
         // vm creation is finished, and with error happens, throw exception here to stop following steps
         throw ClusteringServiceException.VM_CREATION_FAILED(clusterName);
      }

      if (created != null) {
         // only when node is created, we need to verify node status
         String verifyScope =
            getJobParameters(chunkContext).getString(
                  JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM);
         if (verifyScope != null && verifyScope.equals(
               JobConstants.GROUP_NODE_SCOPE_VALUE)) {
            String groupName =
               getJobParameters(chunkContext).getString(
                     JobConstants.GROUP_NAME_JOB_PARAM);
            long oldInstanceNum =
               getJobParameters(chunkContext).getLong(
                     JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);
            verifyGroupVmReady(clusterName, groupName, oldInstanceNum);
         } else {
            verifyAllVmReady(clusterName);
         }
      }
      updateVhmMasterMoid(clusterName);
      return RepeatStatus.FINISHED;
   }

   private void verifyGroupVmReady(String clusterName, String groupName,
         long oldInstanceNum) {
      if (groupName == null) {
         logger.info("No group name specified, ignore node status verification.");
         return;
      }
      List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(
            clusterName, groupName);
      List<NodeEntity> toBeVerified = new ArrayList<NodeEntity>();
      for (NodeEntity node : nodes) {
         long index = JobUtils.getVmIndex(node.getVmName());
         if (index < oldInstanceNum) {
            // do not verify existing nodes from last successful deployment
            continue;
         }
         toBeVerified.add(node);
      }
      JobUtils.verifyNodesStatus(toBeVerified, NodeStatus.VM_READY, false);
   }

   private void verifyAllVmReady(String clusterName) {
      List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(clusterName);
      JobUtils.verifyNodesStatus(nodes, NodeStatus.VM_READY, false);
   }

   /**
    * Add successfully created node, which information is got from vc creation.
    * If deleted any VM, or nodes during vm creation step, which may violate
    * placement policy. We'll remove the node if it's not re-created.
    *
    * @param clusterName
    * @param addedNodes
    * @param deletedNodeNames
    * @return
    */
   public void addNodeToMetaData(String clusterName,
         List<BaseNode> addedNodes, Set<String> deletedNodeNames) {
      if (addedNodes == null || addedNodes.isEmpty()) {
         logger.info("No node is added!");
         return;
      }

      for (BaseNode vNode : addedNodes) {
         deletedNodeNames.remove(vNode.getVmName());
         replaceNodeEntity(vNode);
      }
   }

   public void removeDeletedNode(final String clusterName,
         final Set<String> deletedNodeNames) {
      if (deletedNodeNames.isEmpty()) {
         return;
      }

      List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(clusterName);
      for (NodeEntity node : nodes) {
         if (deletedNodeNames.contains(node.getVmName())) {
            logger.info("Remove Node " + node.getVmName() + " from meta db.");
            getClusterEntityMgr().delete(node);
         }
      }
   }
   
   private void updateVhmMasterMoid(String clusterName) {
      ClusterEntity cluster = getClusterEntityMgr().findByName(clusterName);
      if (cluster.getVhmMasterMoid() == null) {
         List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(clusterName);
         for (NodeEntity node: nodes) {
            if(node.getMoId() != null && node.getNodeGroup().getRoles() != null) {
               @SuppressWarnings("unchecked")
               List<String> roles = new Gson().fromJson(node.getNodeGroup().getRoles(), List.class);
               if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
                  cluster.setVhmMasterMoid(node.getMoId());
                  break;
              }
            }
         }
      }
   }

   @Transactional
   private void replaceNodeEntity(BaseNode vNode) {
      logger.info("Add or replace node info for VM " + vNode.getVmName());
      ClusterEntity cluster = getClusterEntityMgr().findByName(vNode.getClusterName());
      AuAssert.check(cluster != null);
      NodeGroupEntity nodeGroupEntity = getClusterEntityMgr().findByName(
            vNode.getClusterName(), vNode.getGroupName());
      AuAssert.check(nodeGroupEntity != null);
      if (nodeGroupEntity.getNodes() == null) {
         nodeGroupEntity.setNodes(new HashSet<NodeEntity>());
      }

      boolean insert = false;
      NodeEntity nodeEntity =
            getClusterEntityMgr()
                  .findByName(nodeGroupEntity, vNode.getVmName());
      // if node already exists, replace the value with new one from vNode.
      if (nodeEntity == null) {
         nodeEntity = new NodeEntity();
         nodeGroupEntity.getNodes().add(nodeEntity);
         insert = true;
      }
      nodeEntity.setVmName(vNode.getVmName());
      setNodeStatus(nodeEntity, vNode);
      if (vNode.getVmMobId() != null) {
         nodeEntity.setMoId(vNode.getVmMobId());
         nodeEntity.setRack(vNode.getTargetRack());
         nodeEntity.setHostName(vNode.getTargetHost());
         nodeEntity.setIpAddress(vNode.getIpAddress());
         nodeEntity.setGuestHostName(vNode.getGuestHostName());

         //set vc resource pool entity
         nodeEntity.setVcRp(rpDao.findByClusterAndRp(
               vNode.getTargetVcCluster(), vNode.getTargetRp()));
         //set vc datastore name list
         Set<String> dsNames = new HashSet<String>();
         for (Disk disk : vNode.getVmSchema().diskSchema.getDisks()) {
            dsNames.add(disk.datastore);
         }
         if (!dsNames.contains(vNode.getTargetDs())) {
            dsNames.add(vNode.getTargetDs());
         }
         List<String> dsList = new ArrayList<String>();
         dsList.addAll(dsNames);
         nodeEntity.setDatastoreNameList(dsList);

         // set volumns
         nodeEntity.setVolumns(getNodeVolumes(vNode));
      }
      nodeEntity.setNodeGroup(nodeGroupEntity);

      if (insert) {
         getClusterEntityMgr().insert(nodeEntity);
      } else {
         getClusterEntityMgr().update(nodeEntity);
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

   /**
    * get volumns start from sdc
    *
    * @param vNode
    * @return
    */
   private List<String> getNodeVolumes(BaseNode vNode) {
      List<Disk> disks = vNode.getVmSchema().diskSchema.getDisks();
      // no system disk
      int dataDiskNum = disks.size() - 1;
      List<String> volumes = new ArrayList<String>();
      for (char a = 'c'; a < 'c' + dataDiskNum; a++) {
         volumes.add("/dev/sd" + a);
      }
      return volumes;
   }

}
