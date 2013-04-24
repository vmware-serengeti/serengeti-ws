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
package com.vmware.bdd.service.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.aurora.vc.VcCache;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;

public class VcResourceUtils {

   private static final Logger logger = Logger
         .getLogger(VcResourceUtils.class);


   public static Collection<VcDatastore> findDSInVCByPattern(final String vcDSNamePattern) {
      Collection<VcDatastore> result =
         VcContext.inVcSessionDo(new VcSession<Collection<VcDatastore>>() {
            @Override
            protected Collection<VcDatastore> body() throws Exception {
               Map<String, VcDatastore> dsMap =
                  new HashMap<String, VcDatastore>();

               List<VcCluster> vcClusters = VcInventory.getClusters();
               for (VcCluster vcCluster : vcClusters) {
                  for (VcDatastore vcDS : vcCluster.getAllDatastores()) {
                     String pattern = CommonUtil.getDatastoreJavaPattern(vcDSNamePattern);
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
      VcCluster vcCluster =
            VcContext.inVcSessionDo(new VcSession<VcCluster>() {
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

   public static VcVirtualMachine findVmInVcCluster(String vcClusterName, String rpName, String vmName) {
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

   public static Folder findFolderByNameList(final VcDatacenter dc, final List<String> folderNames) {
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

   public static Folder findFolderByName(final Folder folder, final String folderName) {
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
      logger.debug("query folder " + name + " from parent folder " + folder.getName());
      ManagedObjectReference[] children = folder.getChildEntity();
      for (ManagedObjectReference ref : children) {
         logger.debug("parent: " + folder.getName());
         logger.debug("children: type " + ref.getType() + ", value " + ref.getValue() + ", guid " + ref.getServerGuid());
         if (VmodlTypeMap.Factory.getTypeMap().getVmodlType(Folder.class).getWsdlName().equals(ref.getType())) {
            Folder child = MoUtil.getManagedObject(ref);
            if (child.getName().equals(name)) {
               return child;
            }
         }
      }
      return null;
   }

   public static boolean isObjectInFolder(final Folder folder, final String mobId) {
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
            return null;
         }
      });
   }

   public static boolean insidedRootFolder(VcVirtualMachine vm) {
      String root = ConfigInfo.getSerengetiRootFolder();
      List<String> folderNames = new ArrayList<String>();
      folderNames.add(root);
      String[] split = vm.getName().split("-");
      // Serengeti VM name follow format: <clusterName>-<groupName>-<index>
      AuAssert.check(split != null && split.length == 3);
      folderNames.add(split[0]);
      folderNames.add(split[1]);
      Folder folder = null;
      try {
         folder = VcResourceUtils.findFolderByNameList(vm.getDatacenter(), folderNames);
      } catch (Exception e) {
         logger.debug("Failed to find vm folder in root folder.", e);
      }
      if (folder != null) {
         return VcResourceUtils.isObjectInFolder(folder, vm.getId());
      } else {
         return false;
      }
   }
}
