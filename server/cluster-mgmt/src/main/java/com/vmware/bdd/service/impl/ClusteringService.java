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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.specpolicy.GuestMachineIdSpec;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vmware.aurora.composition.CreateVMFolderSP;
import com.vmware.aurora.composition.DeleteVMFolderSP;
import com.vmware.aurora.composition.DiskSchema;
import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.composition.NetworkSchema.Network;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CmsWorker;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcSnapshot;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcevent.VcEventRouter;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.StorageRead.DiskScsiControllerType;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.clone.spec.VmCreateSpec;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.resmgmt.ResourceReservation;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.manager.ElasticityScheduleManager;
import com.vmware.bdd.placement.Container;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.interfaces.IPlacementService;
import com.vmware.bdd.service.IClusterInitializerService;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.job.ClusterNodeUpdator;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.sp.BaseProgressCallback;
import com.vmware.bdd.service.sp.ConfigIOShareSP;
import com.vmware.bdd.service.sp.CreateResourcePoolSP;
import com.vmware.bdd.service.sp.CreateVmPrePowerOn;
import com.vmware.bdd.service.sp.DeleteRpSp;
import com.vmware.bdd.service.sp.DeleteVmByIdSP;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.QueryIpAddress;
import com.vmware.bdd.service.sp.SetAutoElasticitySP;
import com.vmware.bdd.service.sp.StartVmSP;
import com.vmware.bdd.service.sp.StopVmSP;
import com.vmware.bdd.service.sp.UpdateVmProgressCallback;
import com.vmware.bdd.service.sp.VmEventProcessor;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.bdd.vmclone.service.intf.IClusterCloneService;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

public class ClusteringService implements IClusteringService {
   private static final int VC_RP_MAX_NAME_LENGTH = 80;
   private static final Logger logger = Logger
         .getLogger(ClusteringService.class);
   private ClusterConfigManager configMgr;

   private ClusterEntityManager clusterEntityMgr;
   private IResourcePoolDAO rpDao;
   private INetworkService networkMgr;
   private IResourceService resMgr;
   private IPlacementService placementService;
   private IClusterInitializerService clusterInitializerService;
   private ElasticityScheduleManager elasticityScheduleMgr;

   private VcVirtualMachine templateVm;
   private BaseNode templateNode;
   private String templateNetworkLabel;
   private static boolean initialized = false;
   private int cloneConcurrency;
   private VmEventProcessor processor;

   private IClusterCloneService cloneService;

   public INetworkService getNetworkMgr() {
      return networkMgr;
   }

   public void setNetworkMgr(INetworkService networkMgr) {
      this.networkMgr = networkMgr;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }

   @Autowired
   public void setResMgr(IResourceService resMgr) {
      this.resMgr = resMgr;
   }

   @Autowired
   public void setPlacementService(IPlacementService placementService) {
      this.placementService = placementService;
   }

   public IClusterInitializerService getClusterInitializerService() {
      return clusterInitializerService;
   }

   @Autowired
   public void setClusterInitializerService(
         IClusterInitializerService clusterInitializerService) {
      this.clusterInitializerService = clusterInitializerService;
   }

   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public IResourcePoolDAO getRpDao() {
      return rpDao;
   }

   @Autowired
   public void setRpDao(IResourcePoolDAO rpDao) {
      this.rpDao = rpDao;
   }

   public String getTemplateVmId() {
      return templateVm.getId();
   }

   public IClusterCloneService getCloneService() {
      return cloneService;
   }

   @Autowired
   public void setCloneService(IClusterCloneService cloneService) {
      this.cloneService = cloneService;
   }

   public ElasticityScheduleManager getElasticityScheduleManager() {
      return elasticityScheduleMgr;
   }

   @Autowired
   public void setElasticityScheduleManager(ElasticityScheduleManager elasticityScheduleMgr) {
      this.elasticityScheduleMgr = elasticityScheduleMgr;
   }

   public synchronized void init() {
      if (!initialized) {
         // XXX hack to approve bootstrap instance id, should be moved out of Configuration
         Configuration
               .approveBootstrapInstanceId(Configuration.BootstrapUsage.ALLOWED);
         Configuration
               .approveBootstrapInstanceId(Configuration.BootstrapUsage.FINALIZED);

         VcContext.initVcContext();
         new VcEventRouter();
         CmsWorker.addPeriodic(new VcInventory.SyncInventoryRequest());
         VcInventory.loadInventory();
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            logger.warn("interupted during sleep " + e.getMessage());
         }
         // add event handler for Serengeti after VC event handler is registered.
         processor = new VmEventProcessor(getClusterEntityMgr());
         processor.installEventHandler();
         processor.start();
         String poolSize =
               Configuration.getNonEmptyString("serengeti.scheduler.poolsize");

         if (poolSize == null) {
            Scheduler.init(Constants.DEFAULT_SCHEDULER_POOL_SIZE,
                  Constants.DEFAULT_SCHEDULER_POOL_SIZE);
         } else {
            Scheduler.init(Integer.parseInt(poolSize),
                  Integer.parseInt(poolSize));
         }

         String concurrency =
               Configuration
                     .getNonEmptyString("serengeti.singlevm.concurrency");
         if (concurrency != null) {
            cloneConcurrency = Integer.parseInt(concurrency);
         } else {
            cloneConcurrency = 1;
         }

         CmsWorker.addPeriodic(new ClusterNodeUpdator(getClusterEntityMgr()));
         snapshotTemplateVM();
         loadTemplateNetworkLable();
         convertTemplateVm();
         clusterInitializerService.transformClusterStatus(
               ClusterStatus.PROVISIONING, ClusterStatus.PROVISION_ERROR);
         elasticityScheduleMgr.start();
         initialized = true;
      }
   }

   synchronized public void destroy() {
      Scheduler.shutdown(true);
      processor.shutdown();
      elasticityScheduleMgr.shutdown();
   }

   private void convertTemplateVm() {
      templateNode = new BaseNode(templateVm.getName());
      List<DiskSpec> diskSpecs = new ArrayList<DiskSpec>();
      for (DeviceId slot : templateVm.getVirtualDiskIds()) {
         VirtualDisk vmdk = (VirtualDisk) templateVm.getVirtualDevice(slot);
         DiskSpec spec = new DiskSpec();
         spec.setSize((int) (vmdk.getCapacityInKB() / (1024 * 1024)));
         spec.setDiskType(DiskType.SYSTEM_DISK);
         spec.setController(DiskScsiControllerType.LSI_CONTROLLER);
         diskSpecs.add(spec);
      }
      templateNode.setDisks(diskSpecs);
   }

   private VcVirtualMachine getTemplateVm() {
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);

      if (ConfigInfo.isDeployAsVApp()) {
         VcResourcePool vApp = serverVm.getParentVApp();
         initUUID(vApp.getName());
         for (VcVirtualMachine vm : vApp.getChildVMs()) {
            // assume only two vm under serengeti vApp, serengeti server and
            // template
            if (!vm.getName().equals(serverVm.getName())) {
               logger.info("got template vm: " + vm.getName());
               return vm;
            }
         }
         return null;
      } else {
         String templateVmName = ConfigInfo.getTemplateVmName();
         logger.info(templateVmName);
         Folder parentFolder = VcResourceUtils.findParentFolderOfVm(serverVm);
         AuAssert.check(parentFolder != null);
         initUUID(parentFolder.getName());
         return VcResourceUtils.findTemplateVmWithinFolder(parentFolder,
               templateVmName);
      }
   }

   private void initUUID(String uuid) {
      if (ConfigInfo.isInitUUID()) {
         ConfigInfo.setSerengetiUUID(uuid);
         ConfigInfo.setInitUUID(false);
         ConfigInfo.save();
      }
   }

   private void snapshotTemplateVM() {
      final VcVirtualMachine templateVM = getTemplateVm();

      if (templateVM == null) {
         throw ClusteringServiceException.TEMPLATE_VM_NOT_FOUND();
      }

      try {
         if (ConfigInfo.isJustUpgraded()) {
            removeRootSnapshot(templateVM);
            ConfigInfo.setJustUpgraded(false);
            ConfigInfo.save();
         }
         this.templateVm = templateVM;
      } catch (Exception e) {
         logger.error("Clustering service initialization error.");
         throw BddException.INTERNAL(e,
               "Clustering service initialization error.");
      }
   }

   private void removeRootSnapshot(final VcVirtualMachine templateVM) {
      VcContext.inVcSessionDo(new VcSession<Boolean>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }

         @Override
         protected Boolean body() throws Exception {
            VcSnapshot snapshot =
                  templateVM.getSnapshotByName(Constants.ROOT_SNAPSTHOT_NAME);
            if (snapshot != null) {
               snapshot.remove();
            }
            return true;
         }
      });
   }

   private void loadTemplateNetworkLable() {
      templateNetworkLabel = VcContext.inVcSessionDo(new VcSession<String>() {
         @Override
         protected String body() throws Exception {
            VirtualDevice[] devices =
                  templateVm.getConfig().getHardware().getDevice();
            for (VirtualDevice device : devices) {
               if (device.getKey() == 4000) {
                  return device.getDeviceInfo().getLabel();
               }
            }
            return null;
         }
      });
   }

   private void updateNicLabels(List<BaseNode> vNodes) {
      logger.info("Start to set network schema for template nic.");
      for (BaseNode node : vNodes) {
         List<Network> networks = node.getVmSchema().networkSchema.networks;
         if (!networks.isEmpty()) {
            networks.get(0).nicLabel = templateNetworkLabel;
            for (int i = 1; i < networks.size(); i++) {
               networks.get(i).nicLabel = VcVmUtil.NIC_LABEL_PREFIX + (i+1);
            }
         }
         logger.info(node.getVmSchema());
      }
   }

   /**
    * cluster create, resize, resume will all call this method for static ip
    * allocation the network contains all allocated ip address to this cluster,
    * so some of them may already be occupied by existing node. So we need to
    * detect if that ip is allocated, before assign that one to one node
    *
    * @param vNodes
    * @param networkAdds
    * @param occupiedIpSets
    */
   private void allocateStaticIp(List<BaseNode> vNodes, List<NetworkAdd> networkAdds,
         Map<String, Set<String>> occupiedIpSets) {
      int i, j;
      for (i = 0; i < networkAdds.size(); i++) {
         NetworkAdd networkAdd = networkAdds.get(i);
         String portGroupName = networkAdd.getPortGroup();
         Set<String> usedIps = null;
         if (occupiedIpSets != null && !occupiedIpSets.isEmpty()) {
            usedIps = occupiedIpSets.get(portGroupName);
         }

         if (networkAdd.isDhcp()) {
            // no need to allocate ip for dhcp
            logger.info("using dhcp for network: " + portGroupName);
         } else {
            logger.info("Start to allocate static ip address for each VM's " + i + "th network.");
            List<String> availableIps =
                  IpBlock.getIpAddressFromIpBlock(networkAdd.getIp());
            if (usedIps != null && !usedIps.isEmpty()) {
               availableIps.removeAll(usedIps);
            }
            AuAssert.check(availableIps.size() == vNodes.size());
            for (j = 0; j < availableIps.size(); j++) {
               vNodes.get(j).updateIpAddressOfPortGroup(portGroupName, availableIps.get(j));
            }
            logger.info("Finished to allocate static ip address for VM's mgr network.");
         }
      }
   }

   private String getMaprActiveJobTrackerIp(final String maprNodeIP,
         final String clusterName) {
      String activeJobTrackerIp = "";
      String errorMsg = "";
      JSch jsch = new JSch();
      String sshUser = Configuration.getString("mapr.ssh.user", "serengeti");
      int sshPort = Configuration.getInt("mapr.ssh.port", 22);
      String prvKeyFile =
            Configuration.getString("serengeti.ssh.private.key.file",
                  "/home/serengeti/.ssh/id_rsa");
      ChannelExec channel = null;
      try {
         Session session = jsch.getSession(sshUser, maprNodeIP, sshPort);
         jsch.addIdentity(prvKeyFile);
         java.util.Properties config = new java.util.Properties();
         config.put("StrictHostKeyChecking", "no");
         session.setConfig(config);
         session.setTimeout(15000);
         session.connect();
         logger.debug("SSH session is connected!");
         channel = (ChannelExec) session.openChannel("exec");
         if (channel != null) {
            logger.debug("SSH channel is connected!");
            StringBuffer buff = new StringBuffer();
            String cmd =
                  "maprcli node list -filter \"[rp==/*]and[svc==jobtracker]\" -columns ip";
            logger.debug("exec command is: " + cmd);
            channel.setPty(true); //to enable sudo
            channel.setCommand("sudo " + cmd);
            BufferedReader in =
                  new BufferedReader(new InputStreamReader(
                        channel.getInputStream()));
            channel.connect();
            if (!canChannelConnect(channel)) {
               errorMsg =
                     "Cannot determine IP address of active JobTracker. No SSH channel connection.";
               logger.error(errorMsg);
               throw BddException.INTERNAL(null, errorMsg);
            }
            while (true) {
               String line = in.readLine();
               buff.append(line);
               logger.debug("jobtracker message: " + line);
               if (channel.isClosed()) {
                  int exitStatus = channel.getExitStatus();
                  logger.debug("Exit status from exec is: " + exitStatus);
                  break;
               }
            }
            in.close();
            Pattern ipPattern = Pattern.compile(Constants.IP_PATTERN);
            Matcher matcher = ipPattern.matcher(buff.toString());
            if (matcher.find()) {
               activeJobTrackerIp = matcher.group();
            } else {
               errorMsg =
                     "Cannot find jobtracker ip info in cluster" + clusterName;
               logger.error(errorMsg);
               throw BddException.INTERNAL(null, errorMsg);
            }
         } else {
            errorMsg = "Get active Jobtracker ip: cannot open SSH channel.";
            logger.error(errorMsg);
            throw BddException.INTERNAL(null, errorMsg);
         }
      } catch (JSchException e) {
         errorMsg = "SSH unknow error: " + e.getMessage();
         logger.error(errorMsg);
         throw BddException.INTERNAL(null, errorMsg);
      } catch (IOException e) {
         errorMsg = "Obtain active jobtracker ip error: " + e.getMessage();
         logger.error(errorMsg);
         throw BddException.INTERNAL(null, errorMsg);
      } finally {
         channel.disconnect();
      }
      return activeJobTrackerIp;
   }

   private boolean canChannelConnect(ChannelExec channel) {
      if (channel == null) {
         return false;
      }
      if (channel.isConnected()) {
         return true;
      }
      try {
         channel.connect();
      } catch (JSchException e) {
         String errorMsg = "SSH connection failed: " + e.getMessage();
         logger.error(errorMsg);
         throw BddException.INTERNAL(null, errorMsg);
      }
      return channel.isConnected();
   }

   private void updateVhmMasterMoid(String clusterName) {
      ClusterEntity cluster = getClusterEntityMgr().findByName(clusterName);
      if (cluster.getVhmMasterMoid() == null) {
         List<NodeEntity> nodes =
               getClusterEntityMgr().findAllNodes(clusterName);
         for (NodeEntity node : nodes) {
            if (node.getMoId() != null
                  && node.getNodeGroup().getRoles() != null) {
               @SuppressWarnings("unchecked")
               List<String> roles =
                     new Gson().fromJson(node.getNodeGroup().getRoles(),
                           List.class);
               if (cluster.getDistro().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
                  if (roles
                        .contains(HadoopRole.MAPR_JOBTRACKER_ROLE.toString())) {
                     String thisJtIp = node.getMgtIp();
                     String activeJtIp;
                     try {
                        activeJtIp =
                              getMaprActiveJobTrackerIp(thisJtIp, clusterName);
                        logger.info("fetched active JT Ip: " + activeJtIp);
                     } catch (Exception e) {
                        continue;
                     }

                     /*
                      * TODO: in multiple NICs env, if portgroups are isolated,
                      * may not able to retrieve active IP.
                      */
                     AuAssert.check(!CommonUtil.isBlank(thisJtIp),
                           "falied to query active JobTracker Ip");
                     for (NodeEntity jt : nodes) {
                        if (jt.getIpAddressSet().contains(activeJtIp)) {
                           cluster.setVhmMasterMoid(jt.getMoId());
                           break;
                        }
                     }
                     break;
                  }
               } else {
                  if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE
                        .toString())) {
                     cluster.setVhmMasterMoid(node.getMoId());
                     break;
                  }
               }
            }
         }
      }
      getClusterEntityMgr().update(cluster);
   }

   @SuppressWarnings("unchecked")
   public boolean setAutoElasticity(String clusterName, boolean refreshAllNodes) {
      logger.info("set auto elasticity for cluster " + clusterName);

      ClusterEntity cluster = getClusterEntityMgr().findByName(clusterName);
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);

      Boolean enableAutoElasticity = cluster.getAutomationEnable();
      if (enableAutoElasticity == null) {
         return true;
      }

      String masterMoId = cluster.getVhmMasterMoid();
      if (masterMoId == null) {
         // this will only occurs when creating cluster
         updateVhmMasterMoid(clusterName);
         cluster = getClusterEntityMgr().findByName(clusterName);
         masterMoId = cluster.getVhmMasterMoid();
         if (masterMoId == null) {
            logger.error("masterMoId missed.");
            throw ClusteringServiceException.SET_AUTO_ELASTICITY_FAILED(cluster
                  .getName());
         }
      }

      String serengetiUUID = ConfigInfo.getSerengetiRootFolder();
      int minComputeNodeNum = cluster.getVhmMinNum();
      int maxComputeNodeNum = cluster.getVhmMaxNum();
      String jobTrackerPort = cluster.getVhmJobTrackerPort();

      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(masterMoId);
      if (vcVm == null) {
         logger.error("cannot find vhm master node");
         return false;
      }
      String masterUUID = vcVm.getConfig().getUuid();

      Callable<Void>[] storeProcedures = new Callable[nodes.size()];
      int i = 0;
      for (NodeEntity node : nodes) {
         VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());
         if (vm == null) {
            logger.error("cannot find node: " + node.getVmName());
            return false;
         }
         if (!refreshAllNodes && !vm.getId().equalsIgnoreCase(masterMoId)) {
            continue;
         }
         List<String> roles =
               new Gson().fromJson(node.getNodeGroup().getRoles(), List.class);
         String distroVendor =
               node.getNodeGroup().getCluster().getDistroVendor();
         boolean isComputeOnlyNode =
               CommonUtil.isComputeOnly(roles, distroVendor);
         SetAutoElasticitySP sp =
               new SetAutoElasticitySP(vm, serengetiUUID, masterMoId,
                     masterUUID, enableAutoElasticity, minComputeNodeNum, maxComputeNodeNum,
                     jobTrackerPort, isComputeOnlyNode);
         storeProcedures[i] = sp;
         i++;
      }
      try {
         // execute store procedures to set auto elasticity
         logger.info("ClusteringService, start to set auto elasticity.");
         boolean success = true;
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProcedures, callback);
         if (result == null) {
            logger.error("set auto elasticity failed.");
            throw ClusteringServiceException
                  .SET_AUTO_ELASTICITY_FAILED(clusterName);
         }
         for (i = 0; i < storeProcedures.length; i++) {
            if (result[i].throwable != null) {
               logger.error("failed to set auto elasticity",
                     result[i].throwable);
               success = false;
            }
         }
         return success;
      } catch (Exception e) {
         logger.error("error in setting auto elasticity", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @SuppressWarnings("unchecked")
   private Map<String, Folder> createVcFolders(ClusterCreate cluster) {
      logger.info("createVcFolders, start to create cluster Folder.");
      // get all nodegroups
      Callable<Void>[] storeProcedures = new Callable[1];
      Folder clusterFolder = null;
      if (cluster.getNodeGroups().length > 0) {
         // create cluster folder first
         NodeGroupCreate group = cluster.getNodeGroups()[0];
         String path = group.getVmFolderPath();
         String[] folderNames = path.split("/");
         List<String> folderList = new ArrayList<String>();
         for (int i = 0; i < folderNames.length - 1; i++) {
            folderList.add(folderNames[i]);
         }
         CreateVMFolderSP sp =
               new CreateVMFolderSP(templateVm.getDatacenter(), null,
                     folderList);
         storeProcedures[0] = sp;
         Map<String, Folder> folders =
               executeFolderCreationProcedures(cluster, storeProcedures);
         for (String name : folders.keySet()) {
            clusterFolder = folders.get(name);
            break;
         }
      }
      logger.info("createVcFolders, start to create group Folders.");
      storeProcedures = new Callable[cluster.getNodeGroups().length];
      int i = 0;
      for (NodeGroupCreate group : cluster.getNodeGroups()) {
         List<String> folderList = new ArrayList<String>();
         folderList.add(group.getName());
         CreateVMFolderSP sp =
               new CreateVMFolderSP(templateVm.getDatacenter(), clusterFolder,
                     folderList);
         storeProcedures[i] = sp;
         i++;
      }
      return executeFolderCreationProcedures(cluster, storeProcedures);
   }

   private Map<String, Folder> executeFolderCreationProcedures(
         ClusterCreate cluster, Callable<Void>[] storeProcedures) {
      Map<String, Folder> folders = new HashMap<String, Folder>();
      try {
         // execute store procedures to create vc folders
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProcedures, callback);
         if (result == null) {
            logger.error("No folder is created.");
            throw ClusteringServiceException.CREATE_FOLDER_FAILED(cluster
                  .getName());
         }

         int total = 0;
         boolean success = true;
         for (int i = 0; i < storeProcedures.length; i++) {
            CreateVMFolderSP sp = (CreateVMFolderSP) storeProcedures[i];
            if (result[i].finished && result[i].throwable == null) {
               ++total;
               Folder childFolder =
                     sp.getResult().get(sp.getResult().size() - 1);
               folders.put(childFolder.getName(), childFolder);
            } else if (result[i].throwable != null) {
               logger.error("Failed to create vm folder", result[i].throwable);
               success = false;
            }
         }
         logger.info(total + " Folders are created.");
         if (!success) {
            throw ClusteringServiceException.CREATE_FOLDER_FAILED(cluster
                  .getName());
         }
         return folders;
      } catch (Exception e) {
         logger.error("error in creating VC folders", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }


   private void executeResourcePoolStoreProcedures(Callable<Void>[] defineSPs,
         String type, String clusterName) throws InterruptedException {
      if (defineSPs.length == 0) {
         logger.debug("no resource pool need to be created.");
         return;
      }
      NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
      ExecutionResult[] result =
            Scheduler.executeStoredProcedures(
                  com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                  defineSPs, callback);
      if (result == null) {
         logger.error("No " + type + " resource pool is created.");
         throw ClusteringServiceException
               .CREATE_RESOURCE_POOL_FAILED(clusterName);
      }
      int total = 0;
      boolean success = true;
      for (int i = 0; i < defineSPs.length; i++) {
         if (result[i].finished && result[i].throwable == null) {
            ++total;
         } else if (result[i].throwable != null) {
            logger.error("Failed to create " + type + " resource pool(s)",
                  result[i].throwable);
            success = false;
         }
      }
      logger.info(total + " " + type + " resource pool(s) are created.");
      if (!success) {
         throw ClusteringServiceException
               .CREATE_RESOURCE_POOL_FAILED(clusterName);
      }
   }

   private Map<String, Integer> collectResourcePoolInfo(List<BaseNode> vNodes,
         final String uuid, Map<String, List<String>> vcClusterRpNamesMap,
         Map<Long, List<NodeGroupCreate>> rpNodeGroupsMap) {
      List<String> resourcePoolNames = null;
      List<NodeGroupCreate> nodeGroups = null;
      int resourcePoolNameCount = 0;
      int nodeGroupNameCount = 0;
      for (BaseNode baseNode : vNodes) {
         String vcCluster = baseNode.getTargetVcCluster();
         VcCluster cluster = VcResourceUtils.findVcCluster(vcCluster);
         if (!cluster.getConfig().getDRSEnabled()) {
            logger.debug("DRS disabled for cluster " + vcCluster
                  + ", do not create child rp for this cluster.");
            continue;
         }
         AuAssert.check(!CommonUtil.isBlank(vcCluster),
               "Vc cluster name cannot be null!");
         if (!vcClusterRpNamesMap.containsKey(vcCluster)) {
            resourcePoolNames = new ArrayList<String>();
         } else {
            resourcePoolNames = vcClusterRpNamesMap.get(vcCluster);
         }
         String vcRp = baseNode.getTargetRp();
         String rpPath = "/" + vcCluster + "/" + vcRp + "/" + uuid;
         long rpHashCode = rpPath.hashCode();
         if (!rpNodeGroupsMap.containsKey(rpHashCode)) {
            nodeGroups = new ArrayList<NodeGroupCreate>();
         } else {
            nodeGroups = rpNodeGroupsMap.get(rpHashCode);
         }
         NodeGroupCreate nodeGroup = baseNode.getNodeGroup();
         if (!getAllNodeGroupNames(nodeGroups).contains(nodeGroup.getName())) {
            nodeGroups.add(nodeGroup);
            rpNodeGroupsMap.put(rpHashCode, nodeGroups);
            nodeGroupNameCount++;
         }
         if (!resourcePoolNames.contains(vcRp)) {
            resourcePoolNames.add(vcRp);
            vcClusterRpNamesMap.put(vcCluster, resourcePoolNames);
            resourcePoolNameCount++;
         }
      }

      Map<String, Integer> countResult = new HashMap<String, Integer>();
      countResult.put("resourcePoolNameCount", resourcePoolNameCount);
      countResult.put("nodeGroupNameCount", nodeGroupNameCount);
      return countResult;
   }

   private List<String> getAllNodeGroupNames(List<NodeGroupCreate> nodeGroups) {
      List<String> nodeGroupNames = new ArrayList<String>();
      for (NodeGroupCreate nodeGroup : nodeGroups) {
         nodeGroupNames.add(nodeGroup.getName());
      }
      return nodeGroupNames;
   }

   private String createVcResourcePools(List<BaseNode> vNodes) {
      logger.info("createVcResourcePools, start to create VC ResourcePool(s).");
      /*
       * define cluster resource pool name.
       */
      String clusterName = vNodes.get(0).getClusterName();
      String uuid = ConfigInfo.getSerengetiUUID();
      String clusterRpName = uuid + "-" + clusterName;
      if (clusterRpName.length() > VC_RP_MAX_NAME_LENGTH) {
         throw ClusteringServiceException.CLUSTER_NAME_TOO_LONG(clusterName);
      }

      /*
       * prepare resource pool names and node group per resource pool for creating cluster
       * resource pools and node group resource pool(s).
       */
      Map<String, List<String>> vcClusterRpNamesMap =
            new HashMap<String, List<String>>();
      Map<Long, List<NodeGroupCreate>> rpNodeGroupsMap =
            new HashMap<Long, List<NodeGroupCreate>>();
      Map<String, Integer> countResult =
            collectResourcePoolInfo(vNodes, uuid, vcClusterRpNamesMap,
                  rpNodeGroupsMap);

      try {
         /*
          * define cluster store procedures of resource pool(s)
          */
         int resourcePoolNameCount = countResult.get("resourcePoolNameCount");
         Callable<Void>[] clusterSPs = new Callable[resourcePoolNameCount];
         int i = 0;
         for (Entry<String, List<String>> vcClusterRpNamesEntry : vcClusterRpNamesMap
               .entrySet()) {
            String vcClusterName = vcClusterRpNamesEntry.getKey();
            VcCluster vcCluster = VcResourceUtils.findVcCluster(vcClusterName);
            if (vcCluster == null) {
               String errorMsg =
                     "Cannot find the vCenter Server cluster " + vcClusterName
                           + ".";
               logger.error(errorMsg);
               throw BddException.INTERNAL(null, errorMsg);
            }
            List<String> resourcePoolNames = vcClusterRpNamesEntry.getValue();
            for (String resourcePoolName : resourcePoolNames) {
               VcResourcePool parentVcResourcePool =
                     VcResourceUtils.findRPInVCCluster(vcClusterName,
                           resourcePoolName);
               if (parentVcResourcePool == null) {
                  String errorMsg =
                        "Cannot find the vCenter Server resource pool "
                              + resourcePoolName + ".";
                  logger.error(errorMsg);
                  throw BddException.INTERNAL(null, errorMsg);
               }
               CreateResourcePoolSP clusterSP =
                     new CreateResourcePoolSP(parentVcResourcePool,
                           clusterRpName);
               clusterSPs[i] = clusterSP;
               i++;
            }
         }

         // execute store procedures to create cluster resource pool(s)
         logger.info("ClusteringService, start to create cluster resource pool(s).");
         executeResourcePoolStoreProcedures(clusterSPs, "cluster", clusterName);

         /*
          * define node group store procedures of resource pool(s)
          */
         int nodeGroupNameCount = countResult.get("nodeGroupNameCount");
         Callable<Void>[] nodeGroupSPs = new Callable[nodeGroupNameCount];
         i = 0;
         for (Entry<String, List<String>> vcClusterRpNamesEntry : vcClusterRpNamesMap
               .entrySet()) {
            String vcClusterName = vcClusterRpNamesEntry.getKey();
            VcCluster vcCluster = VcResourceUtils.findVcCluster(vcClusterName);
            if (vcCluster == null) {
               String errorMsg =
                     "Cannot find the vCenter Server cluster " + vcClusterName
                           + ".";
               logger.error(errorMsg);
               throw BddException.INTERNAL(null, errorMsg);
            }
            if (!vcCluster.getConfig().getDRSEnabled()) {
               continue;
            }
            List<String> resourcePoolNames = vcClusterRpNamesEntry.getValue();
            for (String resourcePoolName : resourcePoolNames) {
               VcResourcePool parentVcResourcePool = null;
               String vcRPName =
                     CommonUtil.isBlank(resourcePoolName) ? clusterRpName
                           : resourcePoolName + "/" + clusterRpName;
               parentVcResourcePool =
                     VcResourceUtils.findRPInVCCluster(vcClusterName, vcRPName);
               if (parentVcResourcePool == null) {
                  String errorMsg =
                        "Cannot find the vCenter Server resource pool "
                              + vcRPName
                              + (CommonUtil.isBlank(resourcePoolName) ? ""
                                    : " in the vCenter Server resource pool "
                                          + resourcePoolName) + ".";
                  logger.error(errorMsg);
                  throw BddException.INTERNAL(null, errorMsg);
               }
               String rpPath =
                     "/" + vcClusterName + "/" + resourcePoolName + "/" + uuid;
               long rpHashCode = rpPath.hashCode();
               for (NodeGroupCreate nodeGroup : rpNodeGroupsMap.get(rpHashCode)) {
                  AuAssert
                        .check(nodeGroup != null,
                              "create node group resource pool failed: node group cannot be null !");
                  if (nodeGroup.getName().length() > 80) {
                     throw ClusteringServiceException
                           .GROUP_NAME_TOO_LONG(nodeGroup.getName());
                  }
                  CreateResourcePoolSP nodeGroupSP =
                        new CreateResourcePoolSP(parentVcResourcePool,
                              nodeGroup.getName(), nodeGroup);
                  nodeGroupSPs[i] = nodeGroupSP;
                  i++;
               }
            }
         }

         //execute store procedures to create node group resource pool(s)
         logger.info("ClusteringService, start to create node group resource pool(s).");
         executeResourcePoolStoreProcedures(nodeGroupSPs, "node group",
               clusterName);
      } catch (Exception e) {
         logger.error("error in creating VC ResourcePool(s)", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
      return clusterRpName;
   }

   @SuppressWarnings("unchecked")
   @Override
   public boolean createVcVms(List<NetworkAdd> networkAdds, List<BaseNode> vNodes,
         Map<String, Set<String>> occupiedIpSets, StatusUpdater statusUpdator) {
      if (vNodes.isEmpty()) {
         logger.info("No vm to be created.");
         return true;
      }
      updateNicLabels(vNodes);
      allocateStaticIp(vNodes, networkAdds, occupiedIpSets);
      Map<String, Folder> folders = createVcFolders(vNodes.get(0).getCluster());
      String clusterRpName = createVcResourcePools(vNodes);
      logger.info("syncCreateVMs, start to create VMs.");

      // update vm info in vc cache, in case snapshot is removed by others
      VcVmUtil.updateVm(templateVm.getId());

      VmCreateSpec sourceSpec = new VmCreateSpec();
      sourceSpec.setVmId(templateVm.getId());
      sourceSpec.setVmName(templateVm.getName());
      sourceSpec.setTargetHost(templateVm.getHost());

      List<VmCreateSpec> specs = new ArrayList<VmCreateSpec>();
      Map<String, BaseNode> nodeMap = new HashMap<String, BaseNode>();
      for (BaseNode vNode : vNodes) {
         // prepare for cloning result
         nodeMap.put(vNode.getVmName(), vNode);
         vNode.setSuccess(false);
         vNode.setFinished(true);
         // generate create spec for fast clone
         VmCreateSpec spec = new VmCreateSpec();
         VmSchema createSchema = getVmSchema(vNode);
         spec.setSchema(createSchema);
         GuestMachineIdSpec machineIdSpec = new GuestMachineIdSpec(
               networkAdds, vNode.fetchPortGroupToIpMap(),
               vNode.getIpConfigs().get(NetConfigInfo.NetTrafficType.MGT_NETWORK).get(0).getPortGroupName());
         logger.info("machine id of vm " + vNode.getVmName() + ":\n" + machineIdSpec.toString());
         spec.setBootupConfigs(machineIdSpec.toGuestVarialbe());
         // timeout is 10 mintues
         QueryIpAddress query =
               new QueryIpAddress(vNode.fetchAllPortGroups(), Constants.VM_POWER_ON_WAITING_SEC);
         spec.setPostPowerOn(query);
         spec.setPrePowerOn(getPrePowerOnFunc(vNode));
         spec.setLinkedClone(false);
         spec.setTargetDs(getVcDatastore(vNode));
         spec.setTargetFolder(folders.get(vNode.getGroupName()));
         spec.setTargetHost(VcResourceUtils.findHost(vNode.getTargetHost()));
         spec.setTargetRp(getVcResourcePool(vNode, clusterRpName));
         spec.setVmName(vNode.getVmName());
         specs.add(spec);
      }

      try {
         UpdateVmProgressCallback callback =
               new UpdateVmProgressCallback(clusterEntityMgr, statusUpdator,
                     vNodes.get(0).getClusterName());

         logger.info("ClusteringService, start to clone template.");
         AuAssert.check(specs.size() > 0);
         VmSchema vmSchema = specs.get(0).getSchema();
         VcVmUtil.checkAndCreateSnapshot(vmSchema);

         // call clone service to copy templates
         List<VmCreateSpec> results =
               cloneService.createCopies(sourceSpec, cloneConcurrency, specs,
                     callback);
         logger.info(results.size() + " VMs are created.");
         boolean success = (specs.size() == results.size());
         for (VmCreateSpec spec : results) {
            BaseNode node = nodeMap.get(spec.getVmName());
            node.setVmMobId(spec.getVmId());
            VcVirtualMachine vm = VcCache.getIgnoreMissing(spec.getVmId());
            if (vm != null) {
               boolean vmSucc = VcVmUtil.setBaseNodeForVm(node, vm);
               if (!vmSucc) {
                  success = false;
               }
            }
            node.setSuccess(success);
         }
         return success;
      } catch (Exception e) {
         logger.error("error in cloning VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   private void setPersistentDiskMode(BaseNode vNode) {
      DiskSchema diskSchema = vNode.getVmSchema().diskSchema;
      if (diskSchema.getDisks() != null) {
         for (Disk disk : diskSchema.getDisks()) {
            disk.mode = DiskMode.persistent;
         }
      }
   }

   private CreateVmPrePowerOn getPrePowerOnFunc(BaseNode vNode) {
      boolean persistentDiskMode = false;

      String haFlag = vNode.getNodeGroup().getHaFlag();
      boolean ha = false;
      boolean ft = false;
      if (haFlag != null && Constants.HA_FLAG_ON.equals(haFlag.toLowerCase())) {
         ha = true;
      }
      if (haFlag != null && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         ha = true;
         ft = true;
         if (vNode.getNodeGroup().getCpuNum() > 1) {
            throw ClusteringServiceException.CPU_NUMBER_MORE_THAN_ONE(vNode
                  .getVmName());
         }

         logger.debug("ft is enabled is for VM " + vNode.getVmName());
         logger.debug("set disk mode to persistent for VM " + vNode.getVmName());
         // change disk mode to persistent, instead of independent_persistent, since FT requires this
         persistentDiskMode = true;
      }

      List<String> roles = vNode.getNodeGroup().getRoles();
      if (roles != null && HadoopRole.hasMgmtRole(roles)) {
         logger.debug(vNode.getVmName() + " is a master node");
         logger.debug("set disk mode to persistent for VM " + vNode.getVmName());
         // change disk mode to persistent, instead of independent_persistent, to allow snapshot and clone on VM
         persistentDiskMode = true;
      }

      if (persistentDiskMode) {
         setPersistentDiskMode(vNode);
      }

      ClusterEntity clusterEntity =
            getClusterEntityMgr().findByName(vNode.getClusterName());
      CreateVmPrePowerOn prePowerOn =
            new CreateVmPrePowerOn(ha, ft, clusterEntity.getIoShares());

      return prePowerOn;
   }

   private static VcDatastore getVcDatastore(BaseNode vNode) {
      VcDatastore ds = VcResourceUtils.findDSInVcByName(vNode.getTargetDs());
      if (ds != null) {
         return ds;
      }
      logger.error("target data store " + vNode.getTargetDs()
            + " is not found.");
      throw ClusteringServiceException.TARGET_VC_DATASTORE_NOT_FOUND(vNode
            .getTargetDs());
   }

   private VcResourcePool getVcResourcePool(BaseNode vNode,
         final String clusterRpName) {
      try {
         String vcRPName = "";
         VcCluster cluster =
               VcResourceUtils.findVcCluster(vNode.getTargetVcCluster());
         if (!cluster.getConfig().getDRSEnabled()) {
            logger.debug("DRS disabled for cluster "
                  + vNode.getTargetVcCluster()
                  + ", put VM under cluster directly.");
            return cluster.getRootRP();
         }
         if (CommonUtil.isBlank(vNode.getTargetRp())) {
            vcRPName = clusterRpName + "/" + vNode.getNodeGroup().getName();
         } else {
            vcRPName =
                  vNode.getTargetRp() + "/" + clusterRpName + "/"
                        + vNode.getNodeGroup().getName();
         }
         VcResourcePool rp =
               VcResourceUtils.findRPInVCCluster(vNode.getTargetVcCluster(),
                     vcRPName);
         if (rp == null) {
            throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
                  vNode.getTargetVcCluster(), vNode.getTargetRp());
         }
         return rp;
      } catch (Exception e) {
         logger.error("Failed to get VC resource pool " + vNode.getTargetRp()
               + " in vc cluster " + vNode.getTargetVcCluster(), e);

         throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
               vNode.getTargetVcCluster(), vNode.getTargetRp());
      }
   }

   private VmSchema getVmSchema(BaseNode vNode) {
      VmSchema schema = vNode.getVmSchema();
      schema.diskSchema.setParent(getTemplateVmId());
      schema.diskSchema.setParentSnap(Constants.ROOT_SNAPSTHOT_NAME);
      return schema;
   }

   @Override
   public List<BaseNode> getPlacementPlan(ClusterCreate clusterSpec,
         List<BaseNode> existedNodes) {
      logger.info("Begin to calculate provision plan.");

      logger.info("Calling resource manager to get available vc hosts");
      Container container = new Container();

      List<VcCluster> clusters = resMgr.getAvailableClusters();
      AuAssert.check(clusters != null && clusters.size() != 0);
      for (VcCluster cl : clusters) {
         VcResourceUtils.refreshDatastore(cl);
         container.addResource(cl);
      }
      container.SetTemplateNode(templateNode);
      if (clusterSpec.getHostToRackMap() != null
            && clusterSpec.getHostToRackMap().size() != 0) {
         container.addRackMap(clusterSpec.getHostToRackMap());
      }

      // rack topology file validation
      Set<String> validRacks = new HashSet<String>();
      List<AbstractHost> hosts = container.getAllHosts();
      for (AbstractHost host : hosts) {
         if (container.getRack(host) != null) {
            // this rack is valid as it contains at least one host
            validRacks.add(container.getRack(host));
         }
      }

      for (NodeGroupCreate nodeGroup : clusterSpec.getNodeGroups()) {
         if (nodeGroup.getPlacementPolicies() != null
               && nodeGroup.getPlacementPolicies().getGroupRacks() != null
               && validRacks.size() == 0) {
            throw PlacementException.INVALID_RACK_INFO(clusterSpec.getName(),
                  nodeGroup.getName());
         }
      }

      List<BaseNode> baseNodes =
            placementService.getPlacementPlan(container, clusterSpec,
                  existedNodes);
      for (BaseNode baseNode : baseNodes) {
         baseNode.setNodeAction(Constants.NODE_ACTION_CLONING_VM);
      }
      logger.info("Finished calculating provision plan");

      return baseNodes;
   }

   @Override
   public boolean startCluster(final String name, StatusUpdater statusUpdator) {
      logger.info("startCluster, start.");
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(name);
      logger.info("startCluster, start to create store procedures.");
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();

      for (int i = 0; i < nodes.size(); i++) {
         NodeEntity node = nodes.get(i);
         if (node.getMoId() == null) {
            logger.info("VC vm does not exist for node: " + node.getVmName());
            continue;
         }
         VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());
         if (vcVm == null) {
            // cannot find VM
            logger.info("VC vm does not exist for node: " + node.getVmName());
            continue;
         }

         QueryIpAddress query = new QueryIpAddress(node.fetchAllPortGroups(), Constants.VM_POWER_ON_WAITING_SEC);
         VcHost host = null;
         if (node.getHostName() != null) {
            host = VcResourceUtils.findHost(node.getHostName());
         }
         StartVmSP startSp = new StartVmSP(vcVm, query, host);
         storeProcedures.add(startSp);
      }

      try {
         if (storeProcedures.isEmpty()) {
            logger.info("no VM is available. Return directly.");
            return true;
         }
         Callable<Void>[] storeProceduresArray =
               storeProcedures.toArray(new Callable[0]);
         // execute store procedures to start VMs
         logger.info("ClusteringService, start to start vms.");
         UpdateVmProgressCallback callback =
               new UpdateVmProgressCallback(clusterEntityMgr, statusUpdator,
                     name);
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProceduresArray, callback);
         if (result == null) {
            logger.error("No VM is started.");
            return false;
         }

         boolean success = true;
         int total = 0;
         for (int i = 0; i < storeProceduresArray.length; i++) {
            if (result[i].finished && result[i].throwable == null) {
               ++total;
            } else if (result[i].throwable != null) {
               StartVmSP sp = (StartVmSP) storeProceduresArray[i];
               VcVirtualMachine vm = sp.getVcVm();
               if (vm != null && VcVmUtil.checkIpAddresses(vm)) {
                  ++total;
               } else {
                  logger.error(
                        "Failed to start VM " + nodes.get(i).getVmName(),
                        result[i].throwable);
                  success = false;
               }
            }
         }
         logger.info(total + " VMs are started.");
         return success;
      } catch (Exception e) {
         logger.error("error in staring VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @Override
   public boolean stopCluster(final String name, StatusUpdater statusUpdator) {
      logger.info("stopCluster, start.");
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(name);
      logger.info("stopCluster, start to create store procedures.");
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      for (int i = 0; i < nodes.size(); i++) {
         NodeEntity node = nodes.get(i);
         if (node.getMoId() == null) {
            logger.info("VC vm does not exist for node: " + node.getVmName());
            continue;
         }
         VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());

         if (vcVm == null) {
            logger.info("VC vm does not exist for node: " + node.getVmName());
            continue;
         }
         StopVmSP stopSp = new StopVmSP(vcVm);
         storeProcedures.add(stopSp);
      }

      try {
         if (storeProcedures.isEmpty()) {
            logger.info("no VM is available. Return directly.");
            return true;
         }
         Callable<Void>[] storeProceduresArray =
               storeProcedures.toArray(new Callable[0]);
         // execute store procedures to start VMs
         logger.info("ClusteringService, start to stop vms.");
         UpdateVmProgressCallback callback =
               new UpdateVmProgressCallback(clusterEntityMgr, statusUpdator,
                     name);
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProceduresArray, callback);
         if (result == null) {
            logger.error("No VM is stoped.");
            return false;
         }

         boolean success = true;
         int total = 0;
         for (int i = 0; i < storeProceduresArray.length; i++) {
            if (result[i].finished && result[i].throwable == null) {
               ++total;
            } else if (result[i].throwable != null) {
               StopVmSP sp = (StopVmSP) storeProceduresArray[i];
               VcVirtualMachine vm = sp.getVcVm();
               if (vm == null || vm.isPoweredOff()) {
                  ++total;
               } else {
                  logger.error("Failed to stop VM " + nodes.get(i).getVmName(),
                        result[i].throwable);
                  success = false;
               }
            }
         }
         logger.info(total + " VMs are stoped.");
         return success;
      } catch (Exception e) {
         logger.error("error in stoping VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   public boolean removeBadNodes(ClusterCreate cluster,
         List<BaseNode> existingNodes, List<BaseNode> deletedNodes,
         Map<String, Set<String>> occupiedIpSets, StatusUpdater statusUpdator) {
      logger.info("Start to remove node violate placement policy "
            + "or in wrong status in cluster: " + cluster.getName());
      // call tm to remove bad nodes
      List<BaseNode> badNodes =
            placementService.getBadNodes(cluster, existingNodes);
      if (badNodes == null) {
         badNodes = new ArrayList<BaseNode>();
      }
      // append node in wrong status
      for (BaseNode node : deletedNodes) {
         if (node.getVmMobId() != null) {
            badNodes.add(node);
         }
      }

      if (badNodes != null && badNodes.size() > 0) {
         boolean deleted = syncDeleteVMs(badNodes, statusUpdator);
         afterBadVcVmDelete(existingNodes, deletedNodes, badNodes, occupiedIpSets);
         return deleted;
      }
      return true;
   }

   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existingNodes) {
      return placementService.getBadNodes(cluster, existingNodes);
   }

   private void afterBadVcVmDelete(List<BaseNode> existingNodes,
         List<BaseNode> deletedNodes, List<BaseNode> vcDeletedNodes,
         Map<String, Set<String>> occupiedIpSets) {
      // clean up in memory node list
      deletedNodes.addAll(vcDeletedNodes);
      Set<String> deletedNodeNames = new HashSet<String>();
      for (BaseNode vNode : vcDeletedNodes) {
         deletedNodeNames.add(vNode.getVmName());
      }
      for (Iterator<BaseNode> ite = existingNodes.iterator(); ite.hasNext();) {
         BaseNode vNode = ite.next();
         if (deletedNodeNames.contains(vNode.getVmName())) {
            JobUtils.adjustOccupiedIpSets(occupiedIpSets, vNode, false);
            ite.remove();
         }
      }
   }

   @Override
   public boolean deleteCluster(final String name, StatusUpdater statusUpdator) {
      logger.info("Start to delete cluster: " + name);
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(name);
      List<BaseNode> vNodes = JobUtils.convertNodeEntities(null, null, nodes);
      boolean deleted = syncDeleteVMs(vNodes, statusUpdator);
      if (nodes.size() > 0) {
         try {
            deleteChildRps(name, vNodes);
         } catch (Exception e) {
            logger.error("ignore delete resource pool error.", e);
         }

         try {
            deleteFolders(vNodes.get(0));
         } catch (Exception e) {
            logger.error("ignore delete folder error.", e);
         }
      }
      return deleted;
   }

   private void deleteChildRps(String hadoopClusterName, List<BaseNode> vNodes) {
      logger.info("Start to delete child resource pools for cluster: "
            + hadoopClusterName);
      Map<String, Map<String, VcResourcePool>> clusterMap =
            new HashMap<String, Map<String, VcResourcePool>>();
      for (BaseNode node : vNodes) {
         String vcClusterName = node.getTargetVcCluster();
         AuAssert.check(vcClusterName != null);
         String vcRpName = node.getTargetRp();
         if (clusterMap.get(vcClusterName) == null) {
            clusterMap
                  .put(vcClusterName, new HashMap<String, VcResourcePool>());
         }
         Map<String, VcResourcePool> rpMap = clusterMap.get(vcClusterName);
         if (rpMap.get(vcRpName) == null) {
            VcResourcePool vcRp =
                  VcResourceUtils.findRPInVCCluster(vcClusterName, vcRpName);
            if (vcRp != null) {
               rpMap.put(vcRpName, vcRp);
            }
         }
      }
      List<VcResourcePool> rps = new ArrayList<VcResourcePool>();
      for (Map<String, VcResourcePool> map : clusterMap.values()) {
         rps.addAll(map.values());
      }
      Callable<Void>[] storedProcedures = new Callable[rps.size()];
      String childRp = ConfigInfo.getSerengetiUUID() + "-" + hadoopClusterName;
      int i = 0;
      for (VcResourcePool rp : rps) {
         DeleteRpSp sp = new DeleteRpSp(rp, childRp);
         storedProcedures[i] = sp;
         i++;
      }
      try {
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storedProcedures, callback);
         if (result == null || result.length == 0) {
            logger.error("No rp is deleted.");
            return;
         }
         int total = 0;
         for (int j = 0; j < storedProcedures.length; j++) {
            if (result[j].throwable != null) {
               DeleteRpSp sp = (DeleteRpSp) storedProcedures[j];
               logger.error(
                     "Failed to delete child resource pool "
                           + sp.getDeleteRpName() + " under " + sp.getVcRp(),
                     result[j].throwable);
            } else {
               total++;
            }
         }
      } catch (Exception e) {
         logger.error("error in deleting resource pools", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   /**
    * this method will delete the cluster root folder, if there is any VM
    * existed and powered on in the folder, the folder deletion will fail.
    *
    * @param node
    * @throws BddException
    */
   private void deleteFolders(BaseNode node) throws BddException {
      String path = node.getVmFolder();
      // path format: <serengeti...>/<cluster name>/<group name>
      String[] folderNames = path.split("/");
      AuAssert.check(folderNames.length == 3);
      VcDatacenter dc = templateVm.getDatacenter();
      List<String> deletedFolders = new ArrayList<String>();
      deletedFolders.add(folderNames[0]);
      deletedFolders.add(folderNames[1]);
      Folder folder = null;
      try {
         folder = VcResourceUtils.findFolderByNameList(dc, deletedFolders);
      } catch (Exception e) {
         logger.error("error in deleting folders", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
      String clusterFolderName = folderNames[0] + "/" + folderNames[1];
      logger.info("find cluster root folder: " + clusterFolderName);
      List<Folder> folders = new ArrayList<Folder>();
      folders.add(folder);
      DeleteVMFolderSP sp = new DeleteVMFolderSP(folders, true, false);
      Callable<Void>[] storedProcedures = new Callable[1];
      storedProcedures[0] = sp;
      try {
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storedProcedures, callback);
         if (result == null || result.length == 0) {
            logger.error("No folder is deleted.");
            return;
         }
         if (result[0].finished && result[0].throwable == null) {
            logger.info("Cluster folder " + clusterFolderName + " is deleted.");
         } else {
            logger.info("Failed to delete cluster folder " + clusterFolderName,
                  result[0].throwable);
         }
      } catch (Exception e) {
         logger.error("error in deleting folders", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @SuppressWarnings("unchecked")
   public boolean syncDeleteVMs(List<BaseNode> badNodes,
         StatusUpdater statusUpdator) {
      logger.info("syncDeleteVMs, start to create store procedures.");
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      for (int i = 0; i < badNodes.size(); i++) {
         BaseNode node = badNodes.get(i);
         if (node.getVmMobId() == null) {
            // vm is already deleted
            continue;
         }
         DeleteVmByIdSP deleteSp = new DeleteVmByIdSP(node.getVmMobId());
         storeProcedures.add(deleteSp);
      }

      try {
         if (storeProcedures.isEmpty()) {
            logger.info("no VM is created. Return directly.");
            return true;
         }
         Callable<Void>[] storeProceduresArray =
               storeProcedures.toArray(new Callable[0]);
         // execute store procedures to delete VMs
         logger.info("ClusteringService, start to delete vms.");
         BaseProgressCallback callback =
               new BaseProgressCallback(statusUpdator, 0, 50);
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProceduresArray, callback);
         if (result == null) {
            logger.error("No VM is deleted.");
            return false;
         }

         int total = 0;
         boolean failed = false;
         for (int i = 0; i < storeProceduresArray.length; i++) {
            BaseNode vNode = badNodes.get(i);
            if (result[i].finished && result[i].throwable == null) {
               vNode.setSuccess(true);
               vNode.setVmMobId(null);
               ++total;
            } else if (result[i].throwable != null) {
               logger.error("Failed to delete VM " + vNode.getVmName(),
                     result[i].throwable);
               vNode.setSuccess(false);
               failed = true;
            }
            vNode.setFinished(true);
         }
         logger.info(total + " VMs are deleted.");
         return !failed;
      } catch (Exception e) {
         logger.error("error in deleting VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @Override
   public UUID reserveResource(String clusterName) {
      ResourceReservation reservation = new ResourceReservation();
      reservation.setClusterName(clusterName);
      return resMgr.reserveResoruce(reservation);
   }

   @Override
   public void commitReservation(UUID reservationId) throws VcProviderException {
      resMgr.commitReservation(reservationId);
   }

   @Override
   public int configIOShares(String clusterName, List<NodeEntity> targetNodes,
         Priority ioShares) {
      AuAssert.check(clusterName != null && targetNodes != null
            && !targetNodes.isEmpty());

      Callable<Void>[] storeProcedures = new Callable[targetNodes.size()];
      int i = 0;
      for (NodeEntity node : targetNodes) {
         ConfigIOShareSP ioShareSP =
               new ConfigIOShareSP(node.getMoId(), ioShares);
         storeProcedures[i] = ioShareSP;
         i++;
      }

      try {
         // execute store procedures to configure io shares
         logger.info("ClusteringService, start to reconfigure vm's io shares.");
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProcedures, callback);
         if (result == null) {
            logger.error("No VM's io share level is reconfigured.");
            throw ClusteringServiceException
                  .RECONFIGURE_IO_SHARE_FAILED(clusterName);
         }

         int total = 0;
         boolean success = true;
         for (i = 0; i < storeProcedures.length; i++) {
            if (result[i].finished && result[i].throwable == null) {
               ++total;
            } else if (result[i].throwable != null) {
               logger.error("Failed to reconfigure vm", result[i].throwable);
               success = false;
            }
         }
         logger.info(total + " vms are reconfigured.");
         if (!success) {
            throw ClusteringServiceException
                  .RECONFIGURE_IO_SHARE_FAILED(clusterName);
         }
         return total;
      } catch (Exception e) {
         logger.error("error in reconfiguring vm io shares", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   @Override
   public boolean startSingleVM(String clusterName, String nodeName,
         StatusUpdater statusUpdator) {
      NodeEntity node = this.clusterEntityMgr.findNodeByName(nodeName);
      QueryIpAddress query = new QueryIpAddress(node.fetchAllPortGroups(), Constants.VM_POWER_ON_WAITING_SEC);

      VcHost host = null;
      if (node.getHostName() != null) {
         host = VcResourceUtils.findHost(node.getHostName());
      }

      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());
      if (vcVm == null) {
         logger.info("VC vm does not exist for node: " + node.getVmName());
         return false;
      }
      StartVmSP startVMSP = new StartVmSP(vcVm, query, host);
      return VcVmUtil.runSPOnSingleVM(node, startVMSP);
   }

   @Override
   public boolean stopSingleVM(String clusterName, String nodeName,
         StatusUpdater statusUpdator, boolean... vmPoweroff) {
      NodeEntity node = this.clusterEntityMgr.findNodeByName(nodeName);
      if (node.getMoId() == null) {
         logger.error("vm mobid for node " + node.getVmName() + " is null");
         return false;
      }
      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());
      if (vcVm == null) {
         logger.info("VC vm does not exist for node: " + node.getVmName());
         return false;
      }
      StopVmSP stopVMSP;
      if (vmPoweroff.length > 0 && vmPoweroff[0]) {
         stopVMSP = new StopVmSP(vcVm, true);
      } else {
         stopVMSP = new StopVmSP(vcVm);
      }
      return VcVmUtil.runSPOnSingleVM(node, stopVMSP);
   }

}
