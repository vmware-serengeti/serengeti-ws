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
package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.gson.internal.Pair;

import com.vmware.aurora.global.Configuration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.composition.compensation.CompensateCreateVmSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VcVmCloneType;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
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
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.ReplaceVmBadDisksSP;
import com.vmware.bdd.service.sp.ReplaceVmPrePowerOn;
import com.vmware.bdd.service.sp.StartVmPostPowerOn;
import com.vmware.bdd.service.sp.StartVmSP;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.specpolicy.GuestMachineIdSpec;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ClusterUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;

@Service
public class ClusterHealService implements IClusterHealService {
   private static final Logger logger = Logger
         .getLogger(ClusterHealService.class);

   private static final String RECOVERY_VM_NAME_POSTFIX = "-recovery";

   private IClusterEntityManager clusterEntityMgr;

   private ClusterConfigManager configMgr;

   private IClusteringService clusteringService;

   private INetworkService networkMgr;

   private final String IMAGESTORE = "IMAGESTORE";

   private final String DISKSTORE = "DISKSTORE";

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
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

   public INetworkService getNetworkMgr() {
      return networkMgr;
   }

   @Autowired
   public void setNetworkMgr(INetworkService networkMgr) {
      this.networkMgr = networkMgr;
   }

   @Override
   public boolean hasBadDisks(String nodeName) {
      return CollectionUtils.isNotEmpty(getBadDisks(nodeName));
   }

   @Override
   public List<DiskSpec> getBadDisks(String nodeName) {
      return getBadDisksByType(nodeName, null);
   }

   @Override
   public boolean hasBadDisksExceptSystem(String nodeName) {

      if (hasBadSystemDisks(nodeName)) {
         return false;
      }

      boolean hasBadDataDisks = CollectionUtils.isNotEmpty(getBadDataDisks(nodeName));

      boolean hasBadSwapDisks = CollectionUtils.isNotEmpty(getBadSwapDisks(nodeName));

      if (hasBadDataDisks || hasBadSwapDisks) {
         return true;
      }

      return false;
   }

   @Override
   public boolean hasBadSystemDisks(String nodeName) {
      return CollectionUtils.isNotEmpty(getBadSystemDisks(nodeName));
   }

   @Override
   public List<DiskSpec> getBadSystemDisks(String nodeName) {
      return getBadDisksByType(nodeName, DiskType.SYSTEM_DISK);
   }

   @Override
   public List<DiskEntity> getBadSystemDiskEntities(String nodeName) {
      return getBadDiskEntitiesByType(nodeName, DiskType.SYSTEM_DISK);
   }

   @Override
   public boolean hasBadSwapDisks(String nodeName) {
      return CollectionUtils.isNotEmpty(getBadSwapDisks(nodeName));
   }

   @Override
   public List<DiskSpec> getBadSwapDisks(String nodeName) {
      return getBadDisksByType(nodeName, DiskType.SWAP_DISK);
   }

   @Override
   public List<DiskEntity> getBadSwapDiskEntities(String nodeName) {
      return getBadDiskEntitiesByType(nodeName, DiskType.SWAP_DISK);
   }

   @Override
   public boolean hasBadDataDisks(String nodeName) {

      if (hasBadSystemDisks(nodeName)) {
         return false;
      }

      return CollectionUtils.isNotEmpty(getBadDataDisks(nodeName));
   }

   @Override
   public List<DiskSpec> getBadDataDisks(String nodeName) {
      return getBadDisksByType(nodeName, DiskType.DATA_DISK);
   }

   @Override
   public List<DiskEntity> getBadDataDiskEntities(String nodeName) {
      return getBadDiskEntitiesByType(nodeName, DiskType.DATA_DISK);
   }

   private List<DiskSpec> getBadDisksByType(String nodeName, DiskType diskType) {
      List<DiskSpec> badDisks = new ArrayList<>();

      List<DiskEntity> badDiskEntities = getBadDiskEntitiesByType(nodeName, diskType);
      for (DiskEntity badDiskEntity : badDiskEntities) {
         badDisks.add(badDiskEntity.toDiskSpec());
      }

      return badDisks;
   }

   private List<DiskEntity> getBadDiskEntitiesByType(String nodeName, DiskType diskType) {
      List<DiskEntity> diskEntities = clusterEntityMgr.getDisks(nodeName);
      List<DiskEntity> badDiskEntities = new ArrayList<>();

      // scan all disks with disk type and filter out those don't have backing vmdk files or
      // whoes vmdk file attaches to unaccessible datastores
      for (DiskEntity diskEntity : diskEntities) {
         if (diskType != null && !diskEntity.getDiskType().equals(diskType.type)) {
            continue;
         }
         if (isBadDisk(diskEntity)) {
            badDiskEntities.add(diskEntity);
         }
      }

      return badDiskEntities;
   }

   private boolean isBadDisk(DiskEntity disk) {
      boolean isBadDisk = false;

      NodeEntity node = disk.getNodeEntity();
      if (node.isNotExist()) {
         logger.info("The VM " + node.getVmName() + " is not exist.");
         return true;
      }

      if (isBadVmdkPath(disk) || isBadDataStore(disk)) {
         isBadDisk = true;
         logger.info("disk " + disk.getName() + " is bad as datastore " + disk.getDatastoreName() + " is not accessible");
      }

      return isBadDisk;
   }

   private boolean isBadDataStore(DiskEntity disk) {
      return (disk.getDatastoreMoId() != null && !VcVmUtil.isDatastoreAccessible(disk.getDatastoreMoId()));
   }

   private boolean isBadVmdkPath(final DiskEntity disk) {

      if (disk.getVmdkPath() == null || disk.getVmdkPath().isEmpty()) {
         return true;
      }

      boolean  isBadVmdkPath = VcContext.inVcSessionDo(new VcSession<Boolean>() {

         @Override
         protected Boolean body() throws Exception {

            DeviceId deviceId = disk.getDiskDeviceId();
            if (deviceId == null) {
               return true;
            }

            VcVirtualMachine vm = VcCache.getIgnoreMissing(disk.getNodeEntity().getMoId());

            VirtualDisk vmdk = (VirtualDisk)vm.getVirtualDevice(deviceId);

            if (vmdk == null || !disk.getVmdkPath().equals(VmConfigUtil.getVmdkPath(vmdk))) {
               return true;
            }

            return false;
         }

         @Override
         protected boolean isTaskSession() {
            return true;
         }

      });

      return isBadVmdkPath;
   }

   private Map<String, List<VcDatastore>> filterDatastores(VcHost targetHost, ClusterCreate spec, String groupName, NodeEntity nodeEntity) {

      Map<String, List<VcDatastore>> candidates = new HashMap<String, List<VcDatastore>>();

      candidates.put(IMAGESTORE, filterDatastoresWithPattern(targetHost, spec, groupName, nodeEntity, true));

      candidates.put(DISKSTORE, filterDatastoresWithPattern(targetHost, spec, groupName, nodeEntity, false));

      return candidates;
   }

   private List<VcDatastore> filterDatastoresWithPattern(VcHost targetHost, ClusterCreate spec, String groupName, NodeEntity nodeEntity, boolean isImagestoreNamePattern) {

      NodeGroupCreate groupSpec = spec.getNodeGroup(groupName);

      String[] datastoreNamePatterns = null;
      if (isImagestoreNamePattern) {
         datastoreNamePatterns = NodeGroupCreate.getImagestoreNamePattern(spec, groupSpec);
      } else {
         datastoreNamePatterns = NodeGroupCreate.getDiskstoreNamePattern(spec, groupSpec);
      }

      List<VcDatastore> candidates = new ArrayList<VcDatastore>();
      for (VcDatastore ds : targetHost.getDatastores()) {
         if (!ds.isAccessible()) {
            continue;
         }
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

   private void findReplacementDisks(String nodeName,
         List<DiskSpec> badDisks, Map<AbstractDatastore, Integer> usage) {
      // reverse sort, in descending order
      // bin pack problem, place large disk first.
      Collections.sort(badDisks);

      for (DiskSpec disk : badDisks) {
         int requiredSize = disk.getSize();
         AbstractDatastore ads = getLeastUsedDatastore(usage, requiredSize);
         if (ads == null) {
            throw ClusterHealServiceException.NOT_ENOUGH_STORAGE(nodeName,
                  "Cannot find a datastore with enough space to place disk "
                        + disk.getName() + " of size " + disk.getSize()
                        + " GB");
         }

         disk.setTargetDs(ads.getName());
         disk.setVmdkPath(null);

         // deduct space
         ads.allocate(requiredSize);
         // increase reference by 1
         usage.put(ads, usage.get(ads) + 1);
      }
   }

   @Override
   public List<DiskSpec> getReplacementDisks(String clusterName, String groupName, String nodeName, List<DiskSpec> badDisks) {

      ClusterCreate spec = configMgr.getClusterConfig(clusterName);

      NodeEntity nodeEntity = clusterEntityMgr.findByName(clusterName, groupName, nodeName);

      VcHost targetHost = getTargetHost(nodeEntity);

      List<DiskSpec> replacementDisks = new ArrayList<DiskSpec>();

      Map<String, List<VcDatastore>> validDatastores = filterDatastores(targetHost, spec, groupName, nodeEntity);

      List<DiskSpec> badSystemDisks = new ArrayList<DiskSpec>();
      for (DiskSpec badDisk : badDisks) {
         if (badDisk.isSystemDisk() || badDisk.isSwapDisk()) {
            badSystemDisks.add(badDisk);
         }
      }
      if (!badSystemDisks.isEmpty()) {
         List<DiskSpec> replacementSystemDisks = replacementDisks(clusterName, groupName, nodeName, badSystemDisks, targetHost, validDatastores.get(IMAGESTORE));
         replacementDisks.addAll(replacementSystemDisks);
      }

      List<DiskSpec> badDataDisks = new ArrayList<DiskSpec>();
      for (DiskSpec badDisk : badDisks) {
         if (badDisk.isDataDisk()) {
            badDataDisks.add(badDisk);
         }
      }
      if (!badDataDisks.isEmpty()) {
         List<DiskSpec> replacementDataDisks = replacementDisks(clusterName, groupName, nodeName, badDataDisks, targetHost, validDatastores.get(DISKSTORE));
         replacementDisks.addAll(replacementDataDisks);
      }

      return replacementDisks;
   }

   private List<DiskSpec> replacementDisks(String clusterName, String groupName, String nodeName, List<DiskSpec> badDisks, VcHost targetHost, List<VcDatastore> validDatastores) {

      // initialize env for placement algorithm
      int totalSizeInGB = 0;
      Map<AbstractDatastore, Integer> usage = new HashMap<AbstractDatastore, Integer>(validDatastores.size());
      List<AbstractDatastore> pools = new ArrayList<AbstractDatastore>(validDatastores.size());

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
               "" + requiredSizeInGB + " GB storage is required on host "
                     + targetHost.getName() + ", but only " + totalSizeInGB
                     + " GB available");
      }

      List<DiskEntity> goodDisks = clusterEntityMgr.getDisks(nodeName);

      // collects datastore usages
      for (DiskEntity disk : goodDisks) {
         boolean bad = false;
         for (DiskSpec diskSpec : badDisks) {
            if (StringUtils.equals(disk.getName(), diskSpec.getName())) {
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

      findReplacementDisks(nodeName, badDisks, usage);

      return badDisks;
   }

   private VcDatastore getTargetDatastore(List<DiskSpec> fullDiskList) {
      String datastore = null;

      for (DiskSpec disk : fullDiskList) {
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

   private Folder getTargetFolder(final NodeEntity node) {
      return VcContext.inVcSessionDo(new VcSession<Folder>() {
         @Override
         protected Folder body() throws Exception {

            if (node.getMoId() != null) {
               VcVirtualMachine vm = VcCache.get(node.getMoId());
               return vm.getParentFolder();
            }

            String folderPath = node.getNodeGroup().getVmFolderPath();
            try {
               List<String> folderNames = Arrays.asList(folderPath.split("/"));
               AuAssert.check(folderNames.size() == 3);

               VcVirtualMachine templateVm = VcCache.get(node.getNodeGroup().getCluster().getTemplateId());
               VcDatacenter dc = templateVm.getDatacenter();

               return VcResourceUtils.findFolderByNameList(dc, folderNames);
            } catch (Exception e) {
               logger.error("error in finding folder " + folderPath , e);
               throw BddException.INTERNAL(e, e.getMessage());
            }

         }
      });
   }

   private VcHost getTargetHost(NodeEntity node) {
      String targetHostName = node.getHostName();

      VcHost targetHost = VcResourceUtils.findHost(targetHostName);

      if (targetHost == null) {
         logger.error("Cannot find the vCenter Server host " + targetHostName + " for node " + node.getVmName());
         throw ClusterHealServiceException.TARGET_VC_HOST_NOT_FOUND(targetHostName, node.getVmName());
      }

      return targetHost;
   }

   private CreateVmSP getReplacementVmSp(ClusterCreate clusterSpec,
         String groupName, NodeEntity node, List<DiskSpec> fullDiskSet) {
      VmSchema createSchema =
            VcVmUtil.getVmSchema(clusterSpec, groupName, fullDiskSet,
                  node.getNodeGroup().getCluster().getTemplateId(),
                  Constants.ROOT_SNAPSTHOT_NAME);

      Map<String, String> guestVariable = generateMachineId(clusterSpec, node);

      // TODO: rafactor this function
      VcVmUtil.addBootupUUID(guestVariable);

      boolean ha = getHaFlag(clusterSpec, groupName);

      boolean ft = getFtFlag(clusterSpec, groupName);

      boolean isMapDistro = clusterEntityMgr.findByName(clusterSpec.getName()).getDistroVendor().equalsIgnoreCase(Constants.MAPR_VENDOR);

      // delete old vm and rename new vm in the prePowerOn
      ReplaceVmPrePowerOn prePowerOn =
            new ReplaceVmPrePowerOn(node.getMoId(), node.getVmName(),
                  clusterSpec.getNodeGroup(groupName).getStorage().getShares(),
                  createSchema.networkSchema, createSchema.diskSchema, ha, ft, isMapDistro);

      // power on the new vm, but not wait for ip address here. we have startVmStep to wait for ip
      String newVmName = node.getVmName();
      if (node.getMoId() != null && !node.getMoId().isEmpty()) {
         newVmName = node.getVmName() + RECOVERY_VM_NAME_POSTFIX;
      }
      return new CreateVmSP(newVmName,
            createSchema, VcVmUtil.getTargetRp(clusterSpec.getName(),
                  groupName, node), getTargetDatastore(fullDiskSet),
            prePowerOn, null, guestVariable, VcVmCloneType.FULL, true, getTargetFolder(node),
            getTargetHost(node));
   }

   @Override
   @SuppressWarnings("unchecked")
   public VcVirtualMachine createReplacementVm(String clusterName,
         String groupName, String nodeName, List<DiskSpec> replacementDisks) {
      ClusterCreate spec = configMgr.getClusterConfig(clusterName);
      NodeEntity node = clusterEntityMgr.findByName(spec.getName(), groupName, nodeName);

      List<DiskSpec> fullDiskList = getReplacedFullDisks(node.getVmName(), replacementDisks);

      CreateVmSP cloneVmSp = getReplacementVmSp(spec, groupName, node, fullDiskList);

      CompensateCreateVmSP deleteVmSp = new CompensateCreateVmSP(cloneVmSp);

      Pair<Callable<Void>, Callable<Void>>[] storeProcedures = new Pair[1];
      storeProcedures[0] =
            new Pair<Callable<Void>, Callable<Void>>(cloneVmSp, deleteVmSp);

      // execute store procedures to create VMs
      logger.info("ClusterHealService, start to create replacement vm for node "
            + nodeName);
      Pair<ExecutionResult, ExecutionResult>[] result;
      try {
         result =
               Scheduler
               .executeStoredProcedures(
                     com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeProcedures, 1, null);

         if (result == null) {
            logger.error("vm creation failed for node " + nodeName);
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
                  "Failed to create replacement virtual machine for node " + node.getVmName(),
                  pair.first.throwable);

            throw ClusterHealServiceException.FAILED_CREATE_REPLACEMENT_VM(node
                  .getVmName());
         }
      } catch (InterruptedException e) {
         logger.error("error in fixing vm " + nodeName, e);
         throw BddException.INTERNAL(e, e.getMessage());
      }

      return null;
   }

   @Override
   public void updateData(String clusterName, String groupName,
         String nodeName, String newVmId) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);

      logger.info("start update vm id and host info for node " + nodeName);
      VcVirtualMachine vm = ClusterUtil.getVcVm(clusterEntityMgr, node);

      node.setMoId(vm.getId());
      node.setHostName(vm.getHost().getName());
      clusterEntityMgr.update(node);

      logger.info("sync up status for node " + nodeName);
      clusterEntityMgr.syncUpNode(clusterName, nodeName);

      List<DiskEntity> fullDiskSet = clusterEntityMgr.getDisks(nodeName);
      for (DiskEntity disk : fullDiskSet) {
         VcVmUtil.populateDiskInfo(disk, newVmId);
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
      JobUtils.verifyNodeStatus(nodeEntity, NodeStatus.VM_READY, false, clusterEntityMgr);
   }

   @Override
   public void startVm(String nodeName, String vmId, String clusterName) {
      NodeEntity nodeEntity = clusterEntityMgr.findNodeByName(nodeName);
      StartVmPostPowerOn query =
            new StartVmPostPowerOn(nodeEntity.fetchAllPortGroups(),
                  Configuration.getInt(Constants.VM_POWER_ON_WAITING_SEC_KEY, Constants.VM_POWER_ON_WAITING_SEC),
                  clusterEntityMgr);

      VcVirtualMachine vcVm = ClusterUtil.getVcVm(clusterEntityMgr, nodeEntity);

      if (vcVm == null) {
         logger.error("VC vm does not exist for vmId: " + vmId);
      }

      VcHost host = vcVm.getHost();

      Callable<Void>[] storeProceduresArray = new Callable[1];
      storeProceduresArray[0] = new StartVmSP(vcVm, null, query, host);
      NoProgressUpdateCallback callback = new NoProgressUpdateCallback();

      try {
         ExecutionResult[] result =
               Scheduler
               .executeStoredProcedures(
                     com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeProceduresArray, callback);
         if (result == null) {
            logger.error("No result from composition layer");
         } else {
            if (result[0].finished && result[0].throwable == null) {
               logger.info("successfully power on vm for node: " + nodeName);
            } else {
               logger.error("failed in powering on vm for node: " + nodeName,
                     result[0].throwable);
               throw ClusterHealServiceException.FAILED_POWER_ON_VM(nodeName);
            }
         }
      } catch (InterruptedException e) {
         logger.error("error in run operation on vm.", e);
         throw ClusterHealServiceException.INTERNAL(e,
               "error in executing startVmSp");
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   public VcVirtualMachine replaceBadDisksExceptSystem(String clusterName, String groupName, String nodeName, List<DiskSpec> replacementDisks) {

      ClusterCreate spec = configMgr.getClusterConfig(clusterName);
      NodeEntity node = clusterEntityMgr.findByName(spec.getName(), groupName, nodeName);

      List<DiskSpec> fullDiskList = getReplacedFullDisks(node.getVmName(), replacementDisks);

      VmSchema createSchema = VcVmUtil.getVmSchema(spec, groupName, fullDiskList, node.getNodeGroup().getCluster().getTemplateId(), Constants.ROOT_SNAPSTHOT_NAME);
      boolean isMapDistro = clusterEntityMgr.findByName(clusterName).getDistroVendor().equalsIgnoreCase(Constants.MAPR_VENDOR);

      ReplaceVmBadDisksSP replaceVmDisksPrePowerOnSP = new ReplaceVmBadDisksSP(node,
            createSchema.diskSchema, VcVmUtil.getTargetRp(spec.getName(), groupName, node),
            getTargetDatastore(fullDiskList),
            getBadDataDiskEntities(node.getVmName()), isMapDistro);

      try {
         Callable<Void>[] storeProceduresArray = new Callable[1];
         storeProceduresArray[0] = replaceVmDisksPrePowerOnSP;

         ExecutionResult[] result = Scheduler.executeStoredProcedures(com.vmware.aurora.composition.concurrent.Priority.BACKGROUND, storeProceduresArray, null);

         if (result == null) {
            logger.error("Failed to replace bad data disks for node " + nodeName);
            return null;
         }

         Throwable replacedDataDisksVmSpException = result[0].throwable;
         if (result[0].finished && replacedDataDisksVmSpException == null) {

            ReplaceVmBadDisksSP sp = (ReplaceVmBadDisksSP) storeProceduresArray[0];
            VcVirtualMachine vm = sp.getVm();
            AuAssert.check(vm != null);

            return vm;
         } else {
            logger.error("Failed to replace bad data disks for node " + node.getVmName(), replacedDataDisksVmSpException);
            throw ClusterHealServiceException.FAILED_TO_REPLACE_BAD_DATA_DISKS(node.getVmName());
         }

      } catch (InterruptedException e) {
         logger.error("error in fixing vm " + nodeName, e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @Override
   public VcVirtualMachine getFixingVm(String clusterName,
         String groupName, String nodeName) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);

      VcResourcePool rp = VcVmUtil.getTargetRp(clusterName, groupName, node);

      String recoverVmName = node.getVmName() + RECOVERY_VM_NAME_POSTFIX;
      if (node.getMoId() != null) {
         VcVirtualMachine vm = getVmFromVc(node);

         // Destroy recover VM from VC
         destroyVMFromVc(rp, nodeName, recoverVmName);

         if (hasBadDisksExceptSystem(nodeName)) {
            return vm;
         } else {
            return null;
         }

      } else {
         return getFixingVmFromRp(rp, nodeName, recoverVmName);
      }
   }

   private void destroyVMFromVc(VcResourcePool rp, String nodeName, String recoverVmName) {
      VcVirtualMachine recoverVm = VcVmUtil.findVmInRp(rp, recoverVmName);
      if (recoverVm != null) {
         try {
            VcVmUtil.destroyVm(recoverVm.getId(), false);
         } catch (Exception e) {
            logger.error("failed to remove obsolete recovery vm for node " + nodeName);
            throw ClusterHealServiceException.FAILED_DELETE_VM(recoverVmName);
         }
      }
   }

   private VcVirtualMachine getVmFromVc(NodeEntity node) {
      VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());
      // the vm id is null if the vm is removed
      if (vm == null) {
         throw ClusterHealServiceException.ERROR_STATUS(node.getVmName(),
               "Serengeti and VC are inconsistent as vm " + node.getVmName() + " is recorded in Seregeti, but not found in VC.");
      }
      return vm;
   }

   private VcVirtualMachine getFixingVmFromRp(VcResourcePool rp, String nodeName, String recoverVmName) {
      VcVirtualMachine oldVm = VcVmUtil.findVmInRp(rp, nodeName);
      VcVirtualMachine recoverVm = VcVmUtil.findVmInRp(rp, recoverVmName);

      if (oldVm != null && recoverVm != null) {
         destroyVMFromVc(rp, nodeName, recoverVmName);
         logger.info("Delete recover vm if " + "vm " + oldVm.getId() + " and recover vm " + recoverVm.getId() + " both exist");
         return null;
      } else if (oldVm == null && recoverVm == null) {
         logger.info("Original vm and recover vm both not exist.");
         return null;
      } else if (recoverVm != null) {
         logger.info("recover vm " + recoverVm.getId() + " exists, rename it to " + nodeName);
         VcVmUtil.rename(recoverVm.getId(), nodeName);
         return recoverVm;
      } else {
         logger.info("recovery probably failed at power on last time, simply return vm " + oldVm.getId());
         return oldVm;
      }
   }

   // replace bad disks with fixing disk, combining as a new disk set
   private List<DiskSpec> getReplacedFullDisks(String nodeName, List<DiskSpec> replacementDisks) {
      List<DiskSpec> fullDiskList = VcVmUtil.toDiskSpecList(clusterEntityMgr.getDisks(nodeName));

      for(DiskSpec diskSpec : fullDiskList) {
         //find the disk with same id, replace the target Ds
         for(DiskSpec replaceDiskEntity : replacementDisks) {
            if(diskSpec.getId() == replaceDiskEntity.getId()) {
               diskSpec.setTargetDs(replaceDiskEntity.getTargetDs());
               diskSpec.setVmdkPath(replaceDiskEntity.getVmdkPath());
               break;
            }
         }
      }

      return fullDiskList;
   }

   private Map<String, String> generateMachineId(ClusterCreate clusterSpec, NodeEntity node) {

      List<NetworkAdd> networkAdds = clusterSpec.getNetworkings();

      // this getRepalcementVmSP() is not called by any UT, so no need to check NPE for node.getIpConfigsInfo()
      GuestMachineIdSpec machineIdSpec = new GuestMachineIdSpec(networkAdds,
            node.fetchPortGroupToIpMap(), node.getPrimaryMgtNic().getNetworkEntity().getPortGroup(), node, networkMgr);
      logger.info("machine id of vm " + node.getVmName() + ":\n" + machineIdSpec.toString());
      Map<String, String> guestVariable = machineIdSpec.toGuestVariable();

      return guestVariable;
   }

   private boolean getHaFlag(ClusterCreate clusterSpec, String groupName) {

      boolean ha = false;
      String haFlag = clusterSpec.getNodeGroup(groupName).getHaFlag();

      if (haFlag != null && Constants.HA_FLAG_ON.equals(haFlag.toLowerCase())) {
         ha = true;
      }

      if (haFlag != null && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         ha = true;
      }

      return ha;
   }

   private boolean getFtFlag(ClusterCreate clusterSpec, String groupName) {

      boolean ft = false;

      String haFlag = clusterSpec.getNodeGroup(groupName).getHaFlag();
      if (haFlag != null && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         ft = true;
      }

      return ft;
   }
}
