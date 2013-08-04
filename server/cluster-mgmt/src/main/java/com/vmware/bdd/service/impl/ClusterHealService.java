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
package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.composition.compensation.CompensateCreateVmSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterHealServiceException;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.sp.QueryIpAddress;
import com.vmware.bdd.service.sp.ReplaceVmPrePowerOn;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.vim.Folder;

@Service
public class ClusterHealService implements IClusterHealService {
   private static final Logger logger = Logger
         .getLogger(ClusterHealService.class);

   private static final String RECOVERY_VM_NAME_POSTFIX = "-recovery";

   private ClusterEntityManager clusterEntityMgr;

   private ClusterConfigManager configMgr;

   private IClusteringService clusteringService;

   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   @Autowired
   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   @Autowired
   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   @Override
   public boolean hasBadDisks(String nodeName) {
      List<DiskSpec> badDisks = getBadDisks(nodeName);
      if (badDisks != null && !badDisks.isEmpty())
         return true;
      else
         return false;
   }

   @Override
   public List<DiskSpec> getBadDisks(String nodeName) {
      List<DiskEntity> disks = clusterEntityMgr.getDisks(nodeName);
      List<DiskSpec> bads = new ArrayList<DiskSpec>();

      // scan all disks and filter out those don't have backing vmdk files or
      // whoes vmdk file attaches to unaccessible datastores
      for (DiskEntity disk : disks) {
         if (disk.getVmdkPath() == null
               || disk.getVmdkPath().isEmpty()
               || (disk.getDatastoreMoId() != null && !VcVmUtil
                     .isDatastoreAccessible(disk.getDatastoreMoId()))) {
            logger.info("disk " + disk.getName() + " is bad as datastore "
                  + disk.getDatastoreName() + " is not accessible");
            bads.add(disk.toDiskSpec());
         }
      }

      return bads;
   }

   private List<VcDatastore> filterDatastores(VcHost targetHost,
         ClusterCreate spec, String groupName) {
      NodeGroupCreate groupSpec = spec.getNodeGroup(groupName);
      // get the datastore name pattern the node group can use
      // TODO: system datastore for system disk
      String[] datastoreNamePatterns =
            NodeGroupCreate.getDiskstoreNamePattern(spec, groupSpec);

      List<VcDatastore> candidates = new ArrayList<VcDatastore>();
      for (VcDatastore ds : targetHost.getDatastores()) {
         if (!ds.isAccessible())
            continue;
         for (String pattern : datastoreNamePatterns) {
            if (ds.getName().matches(pattern)) {
               candidates.add(ds);
               break;
            }
         }
      }

      return candidates;
   }

   private AbstractDatastore getLeastUsedDatastore(
         Map<AbstractDatastore, Integer> usage, int requiredInGB) {
      int min = Integer.MAX_VALUE;
      AbstractDatastore result = null;

      for (AbstractDatastore ads : usage.keySet()) {
         if (ads.getFreeSpace() >= requiredInGB && usage.get(ads) < min) {
            min = usage.get(ads);
            result = ads;
         }
      }

      return result;
   }

   private List<DiskSpec> findReplacementDisks(String nodeName,
         List<DiskSpec> badDisks, Map<AbstractDatastore, Integer> usage) {
      // reverse sort, in descending order
      // bin pack problem, place large disk first. 
      Collections.sort(badDisks);

      List<DiskSpec> replacements = new ArrayList<DiskSpec>(badDisks.size());

      for (DiskSpec disk : badDisks) {
         int requiredSize = disk.getSize();
         AbstractDatastore ads = getLeastUsedDatastore(usage, requiredSize);
         if (ads == null) {
            throw ClusterHealServiceException.NOT_ENOUGH_STORAGE(nodeName,
                  "can not find datastore with enough space to place disk "
                        + disk.getName() + " with size " + disk.getSize()
                        + " GB");
         }

         DiskSpec replacement = new DiskSpec(disk);
         replacement.setTargetDs(ads.getName());
         replacement.setVmdkPath(null);
         replacements.add(replacement);

         // deduct space
         ads.allocate(requiredSize);
         // increase reference by 1
         usage.put(ads, usage.get(ads) + 1);
      }

      return replacements;
   }

   @Override
   public List<DiskSpec> getReplacementDisks(String clusterName,
         String groupName, String nodeName, List<DiskSpec> badDisks) {
      ClusterCreate spec = configMgr.getClusterConfig(clusterName);

      NodeEntity nodeEntity =
            clusterEntityMgr.findByName(clusterName, groupName, nodeName);
      VcHost targetHost = VcResourceUtils.findHost(nodeEntity.getHostName());

      List<VcDatastore> validDatastores =
            filterDatastores(targetHost, spec, groupName);

      // initialize env for placement algorithm
      int totalSizeInGB = 0;
      Map<AbstractDatastore, Integer> usage =
            new HashMap<AbstractDatastore, Integer>(validDatastores.size());
      List<AbstractDatastore> pools =
            new ArrayList<AbstractDatastore>(validDatastores.size());

      for (VcDatastore ds : validDatastores) {
         totalSizeInGB += ds.getFreeSpace() >> 30;
         AbstractDatastore ads =
               new AbstractDatastore(ds.getName(),
                     (int) (ds.getFreeSpace() >> 30));
         pools.add(ads);
         usage.put(ads, 0);
      }

      int requiredSizeInGB = 0;
      for (DiskSpec disk : badDisks) {
         requiredSizeInGB += disk.getSize();
      }

      if (totalSizeInGB < requiredSizeInGB) {
         throw ClusterHealServiceException.NOT_ENOUGH_STORAGE(nodeName,
               "required " + requiredSizeInGB + " GB storage on host "
                     + targetHost.getName() + ", but only " + totalSizeInGB
                     + " GB available");
      }

      List<DiskEntity> goodDisks = clusterEntityMgr.getDisks(nodeName);

      // collects datastore usages
      for (DiskEntity disk : goodDisks) {
         boolean bad = false;
         for (DiskSpec diskSpec : badDisks) {
            if (disk.getName().equals(diskSpec.getName())) {
               bad = true;
               break;
            }
         }

         // ignore bad disks
         if (bad)
            continue;

         AbstractDatastore targetAds = null;
         for (AbstractDatastore ads : pools) {
            if (ads.getName().equals(disk.getDatastoreName())) {
               targetAds = ads;
               break;
            }
         }

         if (targetAds == null)
            logger.warn("a healthy disk " + disk.getName()
                  + " used a datastore " + disk.getDatastoreName()
                  + "that is not specified in cluster spec");
         else {
            Integer val = usage.get(targetAds);
            usage.put(targetAds, val + 1);
         }
      }

      return findReplacementDisks(nodeName, badDisks, usage);
   }

   private VcDatastore getTargetDatastore(List<DiskSpec> disks) {
      String datastore = null;
      for (DiskSpec disk : disks) {
         if (DiskType.SYSTEM_DISK.equals(disk.getDiskType())) {
            datastore = disk.getTargetDs();
         }
      }
      AuAssert.check(datastore != null);

      VcDatastore ds = VcResourceUtils.findDSInVcByName(datastore);
      if (ds != null) {
         return ds;
      }
      logger.error("target data store " + datastore + " is not found.");
      throw ClusteringServiceException.TARGET_VC_DATASTORE_NOT_FOUND(datastore);
   }

   private Folder getTargetFolder(NodeEntity node, NodeGroupCreate nodeGroup) {
      VcVirtualMachine vm = VcCache.get(node.getMoId());

      String folderPath = nodeGroup.getVmFolderPath();
      List<String> folderNames = Arrays.asList(folderPath.split("/"));
      AuAssert.check(!folderNames.isEmpty());

      return VcResourceUtils.findFolderByNameList(vm.getDatacenter(),
            folderNames);
   }

   private VcHost getTargetHost(NodeEntity node) {
      return VcResourceUtils.findHost(node.getHostName());
   }

   private CreateVmSP getReplacementVmSp(ClusterCreate clusterSpec,
         String groupName, NodeEntity node, List<DiskSpec> fullDiskSet) {
      VmSchema createSchema =
            VcVmUtil.getVmSchema(clusterSpec, groupName, fullDiskSet,
                  clusteringService.getTemplateVmId(),
                  clusteringService.getTemplateSnapId());

      NetworkAdd networkAdd = clusterSpec.getNetworking().get(0);
      Map<String, String> guestVariable =
            ClusteringService.getNetworkGuestVariable(networkAdd,
                  node.getIpAddress(), node.getGuestHostName());
      VcVmUtil.addBootupUUID(guestVariable);

      // delete old vm and rename new vm in the prePowerOn
      ReplaceVmPrePowerOn prePowerOn =
            new ReplaceVmPrePowerOn(node.getMoId(), node.getVmName(),
                  clusterSpec.getNodeGroup(groupName).getStorage().getShares(),
                  fullDiskSet, createSchema.networkSchema);

      // timeout is 10 mintues
      QueryIpAddress query =
            new QueryIpAddress(Constants.VM_POWER_ON_WAITING_SEC);
      return new CreateVmSP(node.getVmName() + RECOVERY_VM_NAME_POSTFIX,
            createSchema, VcVmUtil.getTargetRp(clusterSpec.getName(),
                  groupName, node), getTargetDatastore(fullDiskSet),
            prePowerOn, query, guestVariable, false, getTargetFolder(node,
                  clusterSpec.getNodeGroup(groupName)), getTargetHost(node));
   }

   @Override
   @SuppressWarnings("unchecked")
   public VcVirtualMachine createReplacementVm(String clusterName,
         String groupName, String nodeName, List<DiskSpec> replacementDisks) {
      ClusterCreate spec = configMgr.getClusterConfig(clusterName);
      NodeEntity node =
            clusterEntityMgr.findByName(spec.getName(), groupName, nodeName);

      // replace bad disks with fixing disk, combining as a new disk set
      List<DiskSpec> fullDiskSet = new ArrayList<DiskSpec>();
      for (DiskEntity disk : clusterEntityMgr.getDisks(nodeName)) {
         fullDiskSet.add(disk.toDiskSpec());
      }
      fullDiskSet.removeAll(replacementDisks);
      fullDiskSet.addAll(replacementDisks);

      CreateVmSP cloneVmSp =
            getReplacementVmSp(spec, groupName, node, fullDiskSet);

      CompensateCreateVmSP deleteVmSp = new CompensateCreateVmSP(cloneVmSp);

      Pair<Callable<Void>, Callable<Void>>[] storeProcedures = new Pair[1];
      storeProcedures[0] =
            new Pair<Callable<Void>, Callable<Void>>(cloneVmSp, deleteVmSp);

      // execute store procedures to create VMs
      logger.info("ClusterHealService, start to create replacement vm.");
      Pair<ExecutionResult, ExecutionResult>[] result;
      try {
         result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProcedures, 0, null);

         if (result == null) {
            logger.error("No VM is created.");
            return null;
         }

         Pair<ExecutionResult, ExecutionResult> pair = result[0];
         CreateVmSP sp = (CreateVmSP) storeProcedures[0].first;
         if (pair.first.finished && pair.first.throwable == null
               && pair.second.finished == false) {
            VcVirtualMachine vm = sp.getVM();
            AuAssert.check(vm != null);
            return vm;
         } else if (pair.first.throwable != null) {
            logger.error(
                  "Failed to create replace VM for node " + node.getVmName(),
                  pair.first.throwable);
            throw ClusterHealServiceException.FAILED_CREATE_REPLACEMENT_VM(node
                  .getVmName());
         }
      } catch (InterruptedException e) {
         logger.error("error in creating VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }

      return null;
   }

   public void updateDiskData(String vmId, String nodeName) {
      // refresh node status 
      clusterEntityMgr.refreshNodeByVmName(vmId, nodeName, false);

      List<DiskEntity> fullDiskSet = clusterEntityMgr.getDisks(nodeName);
      for (DiskEntity disk : fullDiskSet) {
         VcVmUtil.populateDiskInfo(disk, vmId);
      }
      clusterEntityMgr.updateDisks(nodeName, fullDiskSet);
   }

   @Override
   public boolean fixDiskFailures(String clusterName, String groupName,
         String nodeName, List<DiskSpec> fullDiskSet) {
      return true;
   }

   @Override
   public void verifyNodeStatus(String vmId, String nodeName) {
      NodeEntity nodeEntity = clusterEntityMgr.findNodeByName(nodeName);
      JobUtils.verifyNodeStatus(nodeEntity, NodeStatus.VM_READY, false);
   }
}
