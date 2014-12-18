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
package com.vmware.bdd.service.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcUtil;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.EnvironmentBrowser;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
import com.vmware.vim.binding.vim.vm.ConfigOption;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;

public class VcResourceUtils {

   private static final Logger logger = Logger.getLogger(VcResourceUtils.class);

   private static final int HARDWARE_VERSION_7 = 7;
   private static final int HARDWARE_VERSION_7_MAX_CPU = 8;
   private static final int HARDWARE_VERSION_7_MAX_MEMORY = 255 * 1024; // 255G
   private static final int HARDWARE_VERSION_8 = 8;
   private static final int HARDWARE_VERSION_8_MAX_CPU = 32;
   private static final int HARDWARE_VERSION_8_MAX_MEMORY = 1011 * 1024; //1011 GB
   private static final int HARDWARE_VERSION_9 = 9;
   private static final int HARDWARE_VERSION_10 = 10;
   private static final int HARDWARE_VERSION_9_AND_10_MAX_CPU = 64;
   private static final int HARDWARE_VERSION_9_AND_10_MAX_MEMORY = 1011 * 1024; //1011 GB

   public static Collection<VcDatastore> findDSInVCByPattern(
         final String vcDSNamePattern) {
      Collection<VcDatastore> result =
            VcContext.inVcSessionDo(new VcSession<Collection<VcDatastore>>() {
               @Override
               protected Collection<VcDatastore> body() throws Exception {
                  Map<String, VcDatastore> dsMap =
                        new HashMap<String, VcDatastore>();

                  List<VcCluster> vcClusters = VcInventory.getClusters();
                  for (VcCluster vcCluster : vcClusters) {
                     for (VcDatastore vcDS : vcCluster.getAllDatastores()) {
                        String pattern = vcDSNamePattern;
                        if (vcDS.getName().matches(pattern)) {
                           dsMap.put(vcDS.getName(), vcDS);
                        }
                     }
                  }
                  return dsMap.values();
               }
            });
      return result;
   }

   public static VcDatastore findDSInVcByName(String dsName) {
      VcDatastore ds = null;
      for (VcCluster cluster : VcInventory.getClusters()) {
         ds = cluster.getDatastore(dsName);
         if (ds != null) {
            return ds;
         }
      }
      return null;
   }

   public static VcNetwork findNetworkInVC(final String portGroupName) {
      return VcContext.inVcSessionDo(new VcSession<VcNetwork>() {

         @Override
         protected VcNetwork body() throws Exception {
            List<VcCluster> vcClusters = VcInventory.getClusters();
            for (VcCluster vcCluster : vcClusters) {
               for (VcNetwork vcNetwork : vcCluster.getAllNetworks()) {
                  if (vcNetwork.getName().equals(portGroupName)) {
                     return vcNetwork;
                  }
               }
            }
            return null;
         }
      });
   }

   public static List<VcHost> findAllHostInVcResourcePool(
         final String vcClusterName, final String vcRpName) {
      Set<VcHost> vcHosts = new HashSet<>();
      VcResourcePool vcResourcePool =
            findRPInVCCluster(vcClusterName, vcRpName);
      collectVcHostFromVcRps(vcHosts, vcResourcePool);
      List<VcHost> hosts = new ArrayList<>();
      hosts.addAll(vcHosts);
      return hosts;
   }

   private static void collectVcHostFromVcRps(Set<VcHost> vcHosts,
         VcResourcePool vcResourcePool) {
      if (vcResourcePool != null) {
         List<VcVirtualMachine> vms = vcResourcePool.getChildVMs();
         if (vms != null && !vms.isEmpty()) {
            for (VcVirtualMachine vm : vms) {
               vcHosts.add(vm.getHost());
            }
         }
         List<VcResourcePool> vcRps = vcResourcePool.getChildren();
         if (vcRps != null && !vcRps.isEmpty()) {
            for (VcResourcePool vcRp : vcRps) {
               collectVcHostFromVcRps(vcHosts, vcRp);
            }
         }
      }
   }

   public static List<VcHost> findAllHostsInVCCluster(final String clusterName) {
      return VcContext.inVcSessionDo(new VcSession<List<VcHost>>() {

         @Override
         protected List<VcHost> body() throws Exception {
            List<VcHost> result = new ArrayList<VcHost>();
            List<VcCluster> vcClusters = VcInventory.getClusters();
            for (VcCluster vcCluster : vcClusters) {
               if (!clusterName.equals(vcCluster.getName())) {
                  continue;
               }
               result.addAll(vcCluster.getHosts());
            }
            return result;
         }
      });
   }

   public static VcHost findHost(final String hostName) {
      return VcContext.inVcSessionDo(new VcSession<VcHost>() {
         @Override
         protected VcHost body() throws Exception {
            List<VcCluster> vcClusters = VcInventory.getClusters();
            for (VcCluster vcCluster : vcClusters) {
               List<VcHost> hosts = vcCluster.getHosts();
               for (VcHost host : hosts) {
                  if (hostName.equals(host.getName())) {
                     return host;
                  }
               }
            }
            return null;
         }
      });
   }

   public static VcCluster findVcCluster(final String clusterName) {
      logger.debug("find vc cluster: " + clusterName);
      VcCluster vcCluster = VcContext.inVcSessionDo(new VcSession<VcCluster>() {
         @Override
         protected VcCluster body() throws Exception {
            List<VcCluster> vcClusters = VcInventory.getClusters();
            for (VcCluster vcCluster : vcClusters) {
               if (clusterName.equals(vcCluster.getName())) {
                  return vcCluster;
               }
            }
            return null;
         }
      });
      return vcCluster;
   }

   /**
    * @param clusterName
    * @param vcRPName
    * @return
    */
   public static VcResourcePool findRPInVCCluster(final String clusterName,
         final String vcRPName) {
      logger.debug("find rp in vc: " + clusterName + "/" + vcRPName);
      VcResourcePool vcRP =
            VcContext.inVcSessionDo(new VcSession<VcResourcePool>() {
               @Override
               protected VcResourcePool body() throws Exception {
                  List<VcCluster> vcClusters = VcInventory.getClusters();
                  String targetRP = "";
                  for (VcCluster vcCluster : vcClusters) {
                     if (!clusterName.equals(vcCluster.getName())) {
                        continue;
                     }
                     if (CommonUtil.isBlank(vcRPName)) {
                        targetRP = "[" + clusterName + "]";
                     } else {
                        targetRP = "[" + clusterName + "]/" + vcRPName;
                     }
                     return vcCluster.searchRP(targetRP);
                  }
                  return null;
               }
            });
      return vcRP;
   }

   public static VcVirtualMachine findVmInVcCluster(String vcClusterName,
         String rpName, String vmName) {
      VcResourcePool rp = findRPInVCCluster(vcClusterName, rpName);
      if (rp == null) {
         return null;
      }
      List<VcVirtualMachine> vms = rp.getChildVMs();
      for (VcVirtualMachine vm : vms) {
         if (vm.getName().equals(vmName)) {
            return vm;
         }
      }
      return null;
   }

   /*
    * Vc has no API to retrieve a VM's parent folder, we have to search from
    * root folder here, and currently, assume Serengeti Server is deployed in a
    * child folder of root.
    */
   public static Folder findParentFolderOfVm(final VcVirtualMachine serverVm) {
      return VcContext.inVcSessionDo(new VcSession<Folder>() {
         @Override
         protected Folder body() throws Exception {
            for (ManagedObjectReference folderRef : serverVm.getDatacenter()
                  .getVmFolder().getChildEntity()) {
               if (VmodlTypeMap.Factory.getTypeMap().getVmodlType(Folder.class)
                     .getWsdlName().equals(folderRef.getType())) {
                  Folder folder = MoUtil.getManagedObject(folderRef);
                  for (ManagedObjectReference vmRef : folder.getChildEntity()) {
                     if (VmodlTypeMap.Factory.getTypeMap()
                           .getVmodlType(VirtualMachine.class).getWsdlName()
                           .equals(vmRef.getType())) {
                        VirtualMachine child = MoUtil.getManagedObject(vmRef);
                        if (child._getRef().equals(serverVm.getMoRef())) {
                           return folder;
                        }
                     }
                  }
               }
            }
            return null;
         }
      });
   }

   public static VcVirtualMachine findTemplateVmWithinFolder(
         final Folder parentFolder, final String templateVmName) {
      logger.info("parent folder name: " + parentFolder.getName());
      return VcContext.inVcSessionDo(new VcSession<VcVirtualMachine>() {
         @Override
         protected VcVirtualMachine body() throws Exception {
            for (ManagedObjectReference vmRef : parentFolder.getChildEntity()) {
               if (VmodlTypeMap.Factory.getTypeMap()
                     .getVmodlType(VirtualMachine.class).getWsdlName()
                     .equals(vmRef.getType())) {
                  VirtualMachine child = MoUtil.getManagedObject(vmRef);
                  if (child.getConfig().getName().equals(templateVmName)) {
                     return (VcVirtualMachine) VcCache.get(child);
                  }
               }
            }
            return null;
         }
      });
   }

   public static Folder findFolderByNameList(final VcDatacenter dc,
         final List<String> folderNames) {
      if (folderNames.isEmpty()) {
         return null;
      }
      return VcContext.inVcSessionDo(new VcSession<Folder>() {
         @Override
         protected Folder body() throws Exception {
            Folder folder = dc.getVmFolder();
            for (String folderName : folderNames) {
               folder = getFolder(folder, folderName);
               if (folder == null) {
                  // do not find one folder in children entity
                  logger.info("Folder " + folderName + " is not found.");
                  break;
               } else {
                  logger.debug("Found folder " + folder.getName());
               }
            }
            return folder;
         }
      });
   }

   public static Folder findFolderByName(final Folder folder,
         final String folderName) {
      if (folderName == null) {
         return null;
      }
      return VcContext.inVcSessionDo(new VcSession<Folder>() {
         @Override
         protected Folder body() throws Exception {
            return getFolder(folder, folderName);
         }
      });
   }

   private static Folder getFolder(Folder folder, String name) throws Exception {
      logger.debug("query folder " + name + " from parent folder "
            + folder.getName());
      ManagedObjectReference[] children = folder.getChildEntity();
      for (ManagedObjectReference ref : children) {
         logger.debug("parent: " + folder.getName());
         logger.debug("children: type " + ref.getType() + ", value "
               + ref.getValue() + ", guid " + ref.getServerGuid());
         if (VmodlTypeMap.Factory.getTypeMap().getVmodlType(Folder.class)
               .getWsdlName().equals(ref.getType())) {
            Folder child = MoUtil.getManagedObject(ref);
            if (child.getName().equals(name)) {
               return child;
            }
         }
      }
      return null;
   }

   public static boolean isObjectInFolder(final Folder folder,
         final String mobId) {
      return VcContext.inVcSessionDo(new VcSession<Boolean>() {
         @Override
         protected Boolean body() throws Exception {
            ManagedObjectReference[] children = folder.getChildEntity();
            for (ManagedObjectReference ref : children) {
               String id = MoUtil.morefToString(ref);
               if (id.equals(mobId)) {
                  return true;
               }
            }
            return false;
         }
      });
   }

   public static void refreshDatastore(final VcCluster cl) {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() {
            try {
               cl.update();
            } catch (Exception e) {
               logger.info("failed to update cluster " + cl.getName()
                     + ", ignore this error.", e);
            }
            List<VcDatastore> dss = cl.getAllDatastores();
            if (dss != null) {
               for (VcDatastore ds : dss) {
                  try {
                     ds.update();
                  } catch (Exception e) {
                     logger.info("failed to update datastore " + ds.getName()
                           + ", ignore this error.", e);
                  }
               }
            }
            try {
               List<VcHost> hosts = cl.getHosts();
               if (hosts != null) {
                  for (VcHost host : hosts) {
                     try {
                        host.update();
                     } catch (Exception e) {
                        logger.info("failed to update host " + host.getName()
                              + ", ignore this error.", e);
                     }
                  }
               }
            } catch (Exception e) {
               logger.info("failed to get host list on cluster " + cl.getName()
                     + ", ignore this error.", e);
            }
            return null;
         }
      });
   }

   public static void refreshResourcePool(final VcCluster cl) {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() {
            try {
               cl.update();
            } catch (Exception e) {
               logger.info("failed to update cluster " + cl.getName()
                     + ", ignore this error.", e);
            }

            List<VcResourcePool> rps = null;
            String rpName = "";
            try {
               rps = cl.getAllRPs();
               if (rps != null) {
                  for (VcResourcePool rp : rps) {
                     rpName = rp.getName();
                     rp.update();
                  }
               }
            } catch (Exception e) {
               if (CommonUtil.isBlank(rpName)) {
                  logger.info("failed to get resource pools from cluster "
                        + cl.getName());
               } else {
                  logger.info("failed to update resource pool " + rpName
                        + ", ignore this error.", e);
               }
            }

            return null;
         }
      });
   }

   public static void refreshNetwork(final VcCluster cl) {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() {
            try {
               cl.update();
            } catch (Exception e) {
               logger.info("failed to update cluster " + cl.getName()
                     + ", ignore this error.", e);
            }
            List<VcNetwork> networks = cl.getAllNetworks();
            if (networks != null) {
               for (VcNetwork network : networks) {
                  try {
                     network.update();
                  } catch (Exception e) {
                     logger.info(
                           "failed to update network " + network.getName()
                                 + ", ignore this error.", e);
                  }
               }
            }
            try {
               List<VcHost> hosts = cl.getHosts();
               if (hosts != null) {
                  for (VcHost host : hosts) {
                     try {
                        host.update();
                     } catch (Exception e) {
                        logger.info("failed to update host " + host.getName()
                              + ", ignore this error.", e);
                     }
                  }
               }
            } catch (Exception e) {
               logger.info("failed to get host list on cluster " + cl.getName()
                     + ", ignore this error.", e);
            }
            return null;
         }
      });
   }

   public static boolean insidedRootFolder(final Folder rootFolder,
         final VcVirtualMachine vm) {
      String[] split = vm.getName().split("-");
      // Serengeti VM name follow format: <clusterName>-<groupName>-<index>
      if (split == null || split.length != 3) {
         logger.debug("VM name does not follow the name format: <clusterName>-<groupName>-<index>, not Serengeti managed VM.");
         return false;
      }
      final String groupFolderName = split[1];
      final String clusterFolderName = split[0];
      return VcContext.inVcSessionDo(new VcSession<Boolean>() {
         @Override
         protected Boolean body() throws Exception {
            try {
               Folder groupFolder = vm.getParentFolder();
               if (groupFolder == null || groupFolder.getName() == null
                     || !groupFolder.getName().equals(groupFolderName)) {
                  logger.debug("VM group folder name mismatch, not Serengeti managed VM.");
                  return false;
               }
               ManagedObjectReference mo = groupFolder.getParent();
               if (mo == null) {
                  logger.debug("VM cluster folder is empty, not Serengeti managed VM.");
                  return false;
               }
               Folder clusterFolder = MoUtil.getManagedObject(mo);
               if (clusterFolder == null || clusterFolder.getName() == null
                     || !clusterFolder.getName().equals(clusterFolderName)) {
                  logger.debug("VM cluster folder name mismatch, not Serengeti managed VM.");
                  return false;
               }
               mo = clusterFolder.getParent();
               if (mo == null) {
                  logger.debug("VM root folder is empty, not Serengeti managed VM.");
                  return false;
               }
               if (MoUtil.morefToString(mo).equals(
                     MoUtil.morefToString(rootFolder._getRef()))) {
                  return true;
               }
            } catch (Exception e) {
               logger.info("Failed to find vm folder in root folder.", e);
            }
            return false;
         }
      });
   }

   public static void checkVmFTAndCpuNumber(final String vmId, final String vmName,
         final int cpuNumber) {
      Boolean ftEnabled = VcContext.inVcSessionDo(new VcSession<Boolean>() {
         @Override
         protected Boolean body() throws Exception {
            final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
            if (vcVm == null) {
               logger.info("vm: " + vmId + " is not found.");
               return false;
            }
            FaultToleranceState ftState = vcVm.getFTState();
            if (FaultToleranceState.notConfigured.ordinal() == ftState
                  .ordinal()) {
               return false;
            }
            return true;
         }
      });
      if (ftEnabled) {
         if (cpuNumber > 1) {
            throw VcProviderException.CPU_EXCEED_ONE(vmName);
         }
      }
   }

   public static void checkVmMaxConfiguration(final String vmId,
         final int cpuNumber, final long memory) {
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      int hardwareVersion = 0;
      if (vcVm == null) {
         logger.info("vm: " + vmId + " is not found.");
         hardwareVersion = -1;
      } else {
         hardwareVersion = VcContext.inVcSessionDo(new VcSession<Integer>() {
            @Override
            protected Integer body() throws Exception {
               VirtualMachine vimVm = vcVm.getManagedObject();
               EnvironmentBrowser envBrowser =
                     MoUtil.getManagedObject(vimVm.getEnvironmentBrowser());
               ConfigOption configOption =
                     envBrowser.queryConfigOption(null, null);
               int hardwareVersion =
                     configOption.getHardwareOptions().getHwVersion();
               logger.info("hardware version is: " + hardwareVersion);
               return hardwareVersion;
            }
         });
      }
      compareMaxConfiguration(vcVm.getName(), hardwareVersion, cpuNumber,
            memory);
   }

   private static void compareMaxConfiguration(String vcVmName,
         int hardwareVersion, int cpuNumber, long memory) {
      if (hardwareVersion == HARDWARE_VERSION_7) {
         if (cpuNumber > HARDWARE_VERSION_7_MAX_CPU) {
            logger.warn("cpu number is greater than :"
                  + HARDWARE_VERSION_7_MAX_CPU);
            throw VcProviderException.CPU_EXCEED_LIMIT(cpuNumber,
                  vcVmName, HARDWARE_VERSION_7_MAX_CPU);
         }
         if (memory > HARDWARE_VERSION_7_MAX_MEMORY) {
            logger.warn("memory is greater than : "
                  + HARDWARE_VERSION_7_MAX_MEMORY);
            throw VcProviderException.MEMORY_EXCEED_LIMIT(
                  HARDWARE_VERSION_7_MAX_MEMORY, vcVmName);
         }
      } else if (hardwareVersion == HARDWARE_VERSION_8) {
         if (cpuNumber > HARDWARE_VERSION_8_MAX_CPU) {
            logger.warn("cpu number is greater than :"
                  + HARDWARE_VERSION_8_MAX_CPU);
            throw VcProviderException.CPU_EXCEED_LIMIT(cpuNumber,
                  vcVmName, HARDWARE_VERSION_8_MAX_CPU);
         }
         if (memory > HARDWARE_VERSION_8_MAX_MEMORY) {
            logger.warn("memory is greater than : "
                  + HARDWARE_VERSION_8_MAX_MEMORY);
            throw VcProviderException.MEMORY_EXCEED_LIMIT(
                  HARDWARE_VERSION_8_MAX_MEMORY, vcVmName);
         }
      } else if (hardwareVersion == HARDWARE_VERSION_9 || hardwareVersion == HARDWARE_VERSION_10) {
         if (cpuNumber > HARDWARE_VERSION_9_AND_10_MAX_CPU) {
            logger.warn("cpu number is greater than :"
                  + HARDWARE_VERSION_9_AND_10_MAX_CPU);
            throw VcProviderException.CPU_EXCEED_LIMIT(cpuNumber,
                  vcVmName, HARDWARE_VERSION_9_AND_10_MAX_CPU);
         }
         if (memory > HARDWARE_VERSION_9_AND_10_MAX_MEMORY) {
            logger.warn("memory is greater than : "
                  + HARDWARE_VERSION_9_AND_10_MAX_MEMORY);
            throw VcProviderException.MEMORY_EXCEED_LIMIT(
                  HARDWARE_VERSION_9_AND_10_MAX_MEMORY, vcVmName);
         }
      }
   }

   public static int getHostTimeDiffInSec(final String hostName) {
      return VcContext.inVcSessionDo(new VcSession<Integer>() {

         @Override
         protected Integer body() throws Exception {
            VcHost vcHost = null;
            List<VcCluster> vcClusters = VcInventory.getClusters();
            for (VcCluster vcCluster : vcClusters) {
               List<VcHost> hosts = vcCluster.getHosts();
               for (VcHost host : hosts) {
                  if (hostName.equals(host.getName())) {
                     vcHost = host;
                     break;
                  }
               }
            }
            return VcUtil.getHostTimeDiffInSec(vcHost);
         }
      });
   }

   public static String getManagementServerHost() {
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         logger.warn("Server MobId (" + Constants.SERENGETI_SERVER_VM_MOBID + ") is missing.");
         return "";
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null)
         return serverVm.getHost().getName();
      else
         return "";
   }
}
