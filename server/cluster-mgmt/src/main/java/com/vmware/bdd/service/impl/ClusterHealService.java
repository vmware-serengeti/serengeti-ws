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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.DiskSchema;
import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.composition.NetworkSchema;
import com.vmware.aurora.composition.NetworkSchema.Network;
import com.vmware.aurora.composition.ResourceSchema;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.composition.compensation.CompensateCreateVmSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.interfaces.model.IDatabaseConfig.Priority;
import com.vmware.aurora.vc.DiskType;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
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
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

@Service
public class ClusterHealService implements IClusterHealService {
   private static final Logger logger = Logger
         .getLogger(ClusterHealService.class);

   private static final String RECOVERY_VM_NAME_POSTFIX = "-recovery";
   private static final String DEFAULT_NIC_1_LABEL = "Network adapter 1";

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
      List<DiskEntity> badDisks = getBadDisks(nodeName);
      if (badDisks != null && !badDisks.isEmpty())
         return true;
      else
         return false;
   }

   @Override
   public List<DiskEntity> getBadDisks(String nodeName) {
      List<DiskEntity> disks = clusterEntityMgr.getDisks(nodeName);
      List<DiskEntity> bads = new ArrayList<DiskEntity>();

      // scan all disks and filter out those don't have backing vmdk files or
      // whoes vmdkf file attaches to unaccessible datastores
      for (DiskEntity disk : disks) {
         if (disk.getVmdkPath() == null
               || disk.getVmdkPath().isEmpty()
               || (disk.getDatastoreMoId() != null && !VcVmUtil
                     .isDatastoreAccessible(disk.getDatastoreMoId()))) {
            logger.info("disk " + disk.getName() + " is bad as datastore "
                  + disk.getDatastoreName() + " is not accessible");
            bads.add(disk);
         }
      }

      return bads;
   }

   private List<VcDatastore> filterDatastores(VcHost targetHost,
         ClusterCreate spec, String groupName) {
      NodeGroupCreate groupSpec = spec.getNodeGroup(groupName);
      // get the datastore name pattern the node group can use
      String[] datastoreNamePatterns =
            NodeGroupCreate.getDatastoreNamePattern(spec, groupSpec);

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

   private List<DiskEntity> findReplacementDisks(String nodeName,
         List<DiskEntity> badDisks, Map<AbstractDatastore, Integer> usage) {
      // reverse sort, in descending order
      Comparator<DiskEntity> cmp = new Comparator<DiskEntity>() {
         @Override
         public int compare(DiskEntity arg0, DiskEntity arg1) {
            if (arg0.getSizeInMB() > arg1.getSizeInMB())
               return -1;
            else if (arg0.getSizeInMB() < arg1.getSizeInMB())
               return 1;

            return 0;
         }
      };
      // bin pack problem, place large disk first. 
      Collections.sort(badDisks, cmp);

      List<DiskEntity> replacements =
            new ArrayList<DiskEntity>(badDisks.size());

      for (DiskEntity disk : badDisks) {
         int requiredSize = disk.getSizeInMB() >> 10;
         AbstractDatastore ads =
               getLeastUsedDatastore(usage, requiredSize);
         if (ads == null) {
            throw ClusterHealServiceException.NOT_ENOUGH_STORAGE(nodeName,
                  "can not find datastore with enough space to place disk "
                        + disk.getName() + " with size " + disk.getSizeInMB()
                        + " MB");
         }

         DiskEntity replacement = disk.copy(disk);
         replacement.setDatastoreName(ads.getName());
         replacement.setDatastoreMoId(null);
         replacement.setVmdkPath(null);
         replacements.add(replacement);

         // increase reference by 1
         ads.allocate(requiredSize);
         usage.put(ads, usage.get(ads) + 1);
      }

      return replacements;
   }

   @Override
   public List<DiskEntity> getReplacementDisks(String clusterName,
         String groupName, String nodeName, List<DiskEntity> badDisks) {
      ClusterCreate spec = configMgr.getClusterConfig(clusterName);

      NodeEntity nodeEntity =
            clusterEntityMgr.findByName(clusterName, groupName, nodeName);
      VcHost targetHost = VcResourceUtils.findHost(nodeEntity.getHostName());

      List<VcDatastore> validDatastores =
            filterDatastores(targetHost, spec, groupName);

      // initialize env for placement algorithm
      int totalSizeInMB = 0;
      Map<AbstractDatastore, Integer> usage =
            new HashMap<AbstractDatastore, Integer>(validDatastores.size());
      List<AbstractDatastore> pools =
            new ArrayList<AbstractDatastore>(validDatastores.size());

      for (VcDatastore ds : validDatastores) {
         totalSizeInMB += ds.getFreeSpace() >> 20;
         AbstractDatastore ads =
               new AbstractDatastore(ds.getName(),
                     (int) (ds.getFreeSpace() >> 30));
         pools.add(ads);
         usage.put(ads, 0);
      }

      int requiredSizeInMB = 0;
      for (DiskEntity disk : badDisks) {
         requiredSizeInMB += disk.getSizeInMB();
      }

      if (totalSizeInMB < requiredSizeInMB) {
         throw ClusterHealServiceException.NOT_ENOUGH_STORAGE(nodeName,
               "required " + requiredSizeInMB + " MB storage on host "
                     + targetHost.getName() + ", but only " + totalSizeInMB
                     + " MB available");
      }

      List<DiskEntity> goodDisks = clusterEntityMgr.getDisks(nodeName);
      goodDisks.removeAll(badDisks);

      for (DiskEntity disk : goodDisks) {
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

   private VmSchema getVmSchema(ClusterCreate spec, String nodeGroup,
         List<DiskEntity> diskSet) {
      NodeGroupCreate groupSpec = spec.getNodeGroup(nodeGroup);

      VmSchema schema = new VmSchema();

      // prepare resource schema
      ResourceSchema resourceSchema = new ResourceSchema();
      resourceSchema.name = "Resource Schema";
      resourceSchema.cpuReservationMHz = 0;
      resourceSchema.memReservationSize = 0;
      resourceSchema.numCPUs = groupSpec.getCpuNum();
      resourceSchema.memSize = groupSpec.getMemCapacityMB();
      resourceSchema.priority = Priority.Normal;
      schema.resourceSchema = resourceSchema;

      // prepare disk schema
      DiskSchema diskSchema = new DiskSchema();
      ArrayList<Disk> disks = new ArrayList<Disk>(diskSet.size());
      for (DiskEntity diskEntity : diskSet) {
         Disk disk = new Disk();
         disk.name = diskEntity.getName();
         disk.type = diskEntity.getDiskType();
         disk.initialSizeMB = diskEntity.getSizeInMB();
         disk.allocationType = diskEntity.getAllocType();
         disk.datastore = diskEntity.getDatastoreName();
         disk.externalAddress = diskEntity.getExternalAddress();
         disk.vmdkPath = diskEntity.getVmdkPath();
         disk.mode = DiskMode.independent_persistent;
         disks.add(disk);
      }
      diskSchema.setParent(clusteringService.getTemplateVmId());
      diskSchema.setParentSnap(clusteringService.getTemplateSnapId());
      diskSchema.setDisks(disks);
      schema.diskSchema = diskSchema;

      // prepare network schema
      Network network = new Network();
      network.vcNetwork = spec.getNetworking().get(0).getPortGroup();
      network.nicLabel = DEFAULT_NIC_1_LABEL;
      ArrayList<Network> networks = new ArrayList<Network>();
      networks.add(network);

      NetworkSchema networkSchema = new NetworkSchema();
      networkSchema.name = "Network Schema";
      networkSchema.networks = networks;
      schema.networkSchema = networkSchema;

      return schema;
   }

   private VcResourcePool getTargetRp(String clusterName, String groupName,
         NodeEntity node) {
      String clusterRpName = ConfigInfo.getSerengetiUUID() + "-" + clusterName;

      VcResourcePoolEntity rpEntity = node.getVcRp();
      String vcRPName = "";

      try {
         VcCluster cluster =
               VcResourceUtils.findVcCluster(rpEntity.getVcCluster());
         if (!cluster.getConfig().getDRSEnabled()) {
            logger.debug("DRS disabled for cluster " + rpEntity.getVcCluster()
                  + ", put VM under cluster directly.");
            return cluster.getRootRP();
         }
         if (CommonUtil.isBlank(rpEntity.getVcResourcePool())) {
            vcRPName = clusterRpName + "/" + groupName;
         } else {
            vcRPName =
                  rpEntity.getVcResourcePool() + "/" + clusterRpName + "/"
                        + groupName;
         }
         VcResourcePool rp =
               VcResourceUtils.findRPInVCCluster(rpEntity.getVcCluster(),
                     vcRPName);
         if (rp == null) {
            throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
                  rpEntity.getVcCluster(), vcRPName);
         }
         return rp;
      } catch (Exception e) {
         logger.error("Failed to get VC resource pool " + vcRPName
               + " in vc cluster " + rpEntity.getVcCluster(), e);

         throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
               rpEntity.getVcCluster(), vcRPName);
      }
   }

   private VcDatastore getTargetDatastore(List<DiskEntity> disks) {
      String datastore = null;
      for (DiskEntity disk : disks) {
         if (DiskType.OS.getTypeName().equals(disk.getDiskType())) {
            datastore = disk.getDatastoreName();
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
         String groupName, NodeEntity node, List<DiskEntity> fullDiskSet) {
      VmSchema createSchema = getVmSchema(clusterSpec, groupName, fullDiskSet);

      NetworkAdd networkAdd = clusterSpec.getNetworking().get(0);
      Map<String, String> guestVariable =
            ClusteringService.getNetworkGuestVariable(networkAdd,
                  node.getIpAddress(), node.getGuestHostName());

      // delete old vm and rename new vm in the prePowerOn
      ReplaceVmPrePowerOn prePowerOn =
            new ReplaceVmPrePowerOn(node.getMoId(), node.getVmName(),
                  clusterSpec.getNodeGroup(groupName).getStorage().getShares());

      // timeout is 10 mintues
      QueryIpAddress query =
            new QueryIpAddress(Constants.VM_POWER_ON_WAITING_SEC);
      return new CreateVmSP(node.getVmName() + RECOVERY_VM_NAME_POSTFIX,
            createSchema, getTargetRp(clusterSpec.getName(), groupName, node),
            getTargetDatastore(fullDiskSet), prePowerOn, query, guestVariable,
            false, getTargetFolder(node, clusterSpec.getNodeGroup(groupName)),
            getTargetHost(node));
   }

   @Override
   @SuppressWarnings("unchecked")
   public VcVirtualMachine createReplacementVm(String clusterName,
         String groupName, String nodeName, List<DiskEntity> fullDiskSet) {
      ClusterCreate spec = configMgr.getClusterConfig(clusterName);
      NodeEntity node =
            clusterEntityMgr.findByName(spec.getName(), groupName, nodeName);

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
            logger.error("Failed to create replace VM for node " + node.getVmName(),
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
         String nodeName, List<DiskEntity> fullDiskSet) {
      return true;
   }
}
