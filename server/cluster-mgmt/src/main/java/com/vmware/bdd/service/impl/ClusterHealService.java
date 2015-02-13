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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.gson.internal.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.composition.compensation.CompensateCreateVmSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VcVmCloneType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterNetConfigInfo;
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
import com.vmware.bdd.service.sp.ReplaceVmPrePowerOn;
import com.vmware.bdd.service.sp.StartVmPostPowerOn;
import com.vmware.bdd.service.sp.StartVmSP;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.specpolicy.GuestMachineIdSpec;
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

   private IClusterEntityManager clusterEntityMgr;

   private ClusterConfigManager configMgr;

   private IClusteringService clusteringService;

   private INetworkService networkMgr;

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
      List<DiskEntity> disks = clusterEntityMgr.getDisks(nodeName);
      List<DiskSpec> bads = new ArrayList<>();

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

   private Folder getTargetFolder(final NodeEntity node) {
      return VcContext.inVcSessionDo(new VcSession<Folder>() {
         @Override
         protected Folder body() throws Exception {
            VcVirtualMachine vm = VcCache.get(node.getMoId());
            return vm.getParentFolder();
         }
      });
   }

   private VcHost getTargetHost(NodeEntity node) {
      return VcResourceUtils.findHost(node.getHostName());
   }

   private CreateVmSP getReplacementVmSp(ClusterCreate clusterSpec,
         String groupName, NodeEntity node, List<DiskSpec> fullDiskSet) {
      VmSchema createSchema =
            VcVmUtil.getVmSchema(clusterSpec, groupName, fullDiskSet,
                  clusteringService.getTemplateVmId(),
                  Constants.ROOT_SNAPSTHOT_NAME);

      List<NetworkAdd> networkAdds = clusterSpec.getNetworkings();

      // this getRepalcementVmSP() is not called by any UT, so no need to check NPE for node.getIpConfigsInfo()
      GuestMachineIdSpec machineIdSpec = new GuestMachineIdSpec(networkAdds,
            node.fetchPortGroupToIpMap(), node.getPrimaryMgtNic().getNetworkEntity().getPortGroup());
      logger.info("machine id of vm " + node.getVmName() + ":\n" + machineIdSpec.toString());
      Map<String, String> guestVariable = machineIdSpec.toGuestVariable();

      // TODO: rafactor this function
      VcVmUtil.addBootupUUID(guestVariable);

      String haFlag = clusterSpec.getNodeGroup(groupName).getHaFlag();
      boolean ha = false;
      boolean ft = false;
      if (haFlag != null && Constants.HA_FLAG_ON.equals(haFlag.toLowerCase())) {
         ha = true;
      }
      if (haFlag != null && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         ha = true;
         ft = true;
      }
      // delete old vm and rename new vm in the prePowerOn
      ReplaceVmPrePowerOn prePowerOn =
            new ReplaceVmPrePowerOn(node.getMoId(), node.getVmName(),
                  clusterSpec.getNodeGroup(groupName).getStorage().getShares(),
                  createSchema.networkSchema, createSchema.diskSchema, ha, ft);

      // power on the new vm, but not wait for ip address here. we have startVmStep to wait for ip
      return new CreateVmSP(node.getVmName() + RECOVERY_VM_NAME_POSTFIX,
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

      // replace bad disks with fixing disk, combining as a new disk set
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

      CreateVmSP cloneVmSp =
            getReplacementVmSp(spec, groupName, node, fullDiskList);

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

   public void updateData(String clusterName, String groupName,
         String nodeName, String newVmId) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);

      logger.info("start update vm id and host info for node " + nodeName);
      VcVirtualMachine vm = VcCache.getIgnoreMissing(newVmId);

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
      JobUtils.verifyNodeStatus(nodeEntity, NodeStatus.VM_READY, false);
   }

   @Override
   public void startVm(String nodeName, String vmId, String clusterName) {
      NodeEntity nodeEntity = clusterEntityMgr.findNodeByName(nodeName);
      StartVmPostPowerOn query =
            new StartVmPostPowerOn(nodeEntity.fetchAllPortGroups(),
                  Constants.VM_POWER_ON_WAITING_SEC, clusterEntityMgr);

      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);

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
   public VcVirtualMachine checkNodeStatus(String clusterName,
         String groupName, String nodeName) {
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);

      VcResourcePool rp = VcVmUtil.getTargetRp(clusterName, groupName, node);

      String recoverVmName = node.getVmName() + RECOVERY_VM_NAME_POSTFIX;
      if (node.getMoId() != null) {
         VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());
         // the vm id is null if the vm is removed
         if (vm == null) {
            throw ClusterHealServiceException.ERROR_STATUS(nodeName,
                  "Serengeti and VC are inconsistent as vm " + nodeName
                        + " is recorded in Seregeti, but not found in VC.");
         }

         VcVirtualMachine recoverVm = VcVmUtil.findVmInRp(rp, recoverVmName);
         if (recoverVm != null) {
            try {
               VcVmUtil.destroyVm(recoverVm.getId(), false);
            } catch (Exception e) {
               logger.error("failed to remove obsolete recovery vm for node "
                     + nodeName);
               throw ClusterHealServiceException
                     .FAILED_DELETE_VM(recoverVmName);
            }
         }

         return null;
      } else {
         VcVirtualMachine oldVm = VcVmUtil.findVmInRp(rp, nodeName);
         VcVirtualMachine recoverVm = VcVmUtil.findVmInRp(rp, recoverVmName);

         if (oldVm != null && recoverVm != null) {
            logger.error("vm " + oldVm.getId() + " and recover vm "
                  + recoverVm.getId() + " both exist, this is not expected!.");
            throw ClusterHealServiceException.ERROR_STATUS(nodeName, "vm "
                  + nodeName + " and its recovery vm " + recoverVmName
                  + " both exist, remove the one you created manually!");
         } else if (oldVm == null && recoverVm == null) {
            logger.error("original vm and recover vm for node " + nodeName
                  + " both missed, this is not expected!.");
            throw ClusterHealServiceException.ERROR_STATUS(nodeName, "vm "
                  + nodeName + " and its recovery vm " + recoverVmName
                  + " both missed, it's not expected!");
         } else if (recoverVm != null) {
            logger.info("recover vm " + recoverVm.getId()
                  + " exists, rename it to " + nodeName);
            VcVmUtil.rename(recoverVm.getId(), nodeName);
            return recoverVm;
         } else {
            logger.info("recovery probably failed at power on last time, simply return vm "
                  + oldVm.getId());
            return oldVm;
         }
      }
   }
}
