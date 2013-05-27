/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.impl;

import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.ScaleServiceException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.IScaleService;
import com.vmware.bdd.service.sp.ScaleVMSP;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ScaleService implements IScaleService {
   private static final Logger logger = Logger.getLogger(ScaleService.class);

   private ClusterEntityManager clusterEntityMgr;

   private ClusterConfigManager clusterConfigMgr;


   /**
    * @return the clusterEntityMgr
    */
   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }


   /**
    * @param clusterEntityMgr
    *           the clusterEntityMgr to set
    */
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public ClusterConfigManager getClusterConfigMgr() {
      return clusterConfigMgr;
   }

   public void setClusterConfigMgr(ClusterConfigManager clusterConfigMgr) {
      this.clusterConfigMgr = clusterConfigMgr;
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.IScaleService#scaleNodeResource(java.lang.String, int, long)
    */
   @Override
   public boolean scaleNodeResource(String nodeName, int cpuNumber, long memory) {
      logger.info("scale node: " + nodeName + ", cpu number: " + cpuNumber
            + ", memory: " + memory);
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);

      DiskEntity swapDisk = findSwapDisk(node);
      VcDatastore targetDs = null;
      long newSwapSizeInMB = 0;
      if (memory > 0) {
         newSwapSizeInMB =
               (((long) Math.ceil(memory * node.getNodeGroup().getSwapRatio()) + 1023) / 1024) * 1024;
         logger.info("new swap disk size(MB): " + newSwapSizeInMB);
         targetDs = getTargetDsForSwapDisk(node, swapDisk, newSwapSizeInMB);
      }

      ScaleVMSP scaleVMSP =
            new ScaleVMSP(node.getMoId(), cpuNumber, memory, targetDs,
                  swapDisk, newSwapSizeInMB);
      boolean vmResult = VcVmUtil.runSPOnSingleVM(node, scaleVMSP);
      return vmResult;
   }

   public DiskEntity findSwapDisk(NodeEntity node) {
      DiskEntity swapDisk = null;
      Set<DiskEntity> diskEntities = node.getDisks();
      for (DiskEntity diskEntity : diskEntities) {
         if (diskEntity.getDiskType().equals(DiskType.SWAP_DISK.getType())) {
            swapDisk = diskEntity;
            break;
         }
      }
      return swapDisk;
   }

   public VcDatastore getTargetDsForSwapDisk(NodeEntity node,
         DiskEntity swapDisk, long newSwapSizeInMB) {
      ClusterCreate clusterSpec =
            clusterConfigMgr.getClusterConfig(node.getNodeGroup().getCluster()
                  .getName());
      NodeGroupCreate ngSpec =
            clusterSpec.getNodeGroup(node.getNodeGroup().getName());

      // use current DS if it has enough space
      VcDatastore currentDs =
            VcResourceUtils.findDSInVcByName(swapDisk.getDatastoreName());
      if (!currentDs.isAccessible()) {
         throw ScaleServiceException.CURRENT_DATASTORE_UNACCESSIBLE(currentDs
               .getName());
      }
      if ((int) (currentDs.getFreeSpace() >> 20) > newSwapSizeInMB
            - swapDisk.getSizeInMB()) {
         return currentDs;
      }

      // else find a valid datastore with largest free space
      VcHost locateHost = VcResourceUtils.findHost(node.getHostName());
      String[] dsNamePatterns =
            NodeGroupCreate.getDatastoreNamePattern(clusterSpec, ngSpec);
      VcDatastore targetDs = null;
      for (VcDatastore ds : locateHost.getDatastores()) {
         if (!ds.isAccessible()) {
            continue;
         }
         for (String pattern : dsNamePatterns) {
            if (ds.getName().matches(pattern)) {
               if (targetDs == null
                     || targetDs.getFreeSpace() < ds.getFreeSpace()) {
                  targetDs = ds;
               }
               break;
            }
         }
      }

      if (targetDs != null
            && (int) (targetDs.getFreeSpace() >> 20) > newSwapSizeInMB) {
         return targetDs;
      }

      logger.warn("cannot find a proper datastore to scale up swap disk of vm: "
            + node.getVmName());

      return null;
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.IScaleService#getVmOriginalCpuNumber(java.lang.String)
    */
   @Override
   public int getVmOriginalCpuNumber(String nodeName) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);
      return node.getCpuNum();
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.IScaleService#getVmOriginalMemory(java.lang.String)
    */
   @Override
   public long getVmOriginalMemory(String nodeName) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);
      return node.getMemorySize();
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.IScaleService#updateSwapDisk(java.lang.String)
    */
   @Override
   public void updateSwapDisk(String nodeName) {
      logger.info("update swap disk meta data for node: " + nodeName);
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);
      DiskEntity swapDisk = findSwapDisk(node);
      VcVmUtil.populateDiskInfo(swapDisk, node.getMoId());
   }

}
