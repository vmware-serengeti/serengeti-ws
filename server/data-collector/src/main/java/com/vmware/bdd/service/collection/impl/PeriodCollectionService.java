/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.collection.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.util.collection.CollectionConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DataObjectType;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.collection.IPeriodCollectionService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class PeriodCollectionService implements IPeriodCollectionService {

   static final Logger logger = Logger
         .getLogger(PeriodCollectionService.class);
   private ICollectionInitializerService collectionInitializerService;
   @Autowired
   protected IResourceInitializerService resourceInitializerService;
   @Autowired
   protected ClusterManager clusterMgr;
   @Autowired
   protected IResourceService resourceService;
   @Autowired
   protected IResourcePoolService resourcePoolService;
   @Autowired
   protected SoftwareManagerCollector softwareManagerCollector;

   @Override
   public Map<String, Map<String, ?>> collectData(DataObjectType dataObjectType) {
      Map<String, Map<String, ?>>  data = new HashMap<String, Map<String, ?>> ();
      switch(dataObjectType) {
      case FOOTPRINT:
         Map<String, Object> footPrintData = getFootprintData();
         data.put(dataObjectType.getName(), footPrintData);
         break;
      case ENVIRONMENTAL_INFORMATION:
         Map<String, Object> environmentalInfoData = getEnvironmentalInfo();
         data.put(dataObjectType.getName(), environmentalInfoData);
         break;
      case COMMONREPORTS:
         Map<String, Object> commonReportsData = getCommonReportsData();
         data.put(dataObjectType.getName(), commonReportsData);
         break;
      default:
      }
      return Collections.unmodifiableMap(data);
   }

   private Map<String, Object> getFootprintData() {
      Map<String, Object> footPrintData = new HashMap<String, Object>();
      footPrintData.put(CollectionConstants.OBJECT_ID, CommonUtil.getUUID());
      footPrintData.put(CollectionConstants.FOOTPRINT_VERSION, getBDEVersion());
      footPrintData.put(CollectionConstants.FOOTPRINT_DEPLOY_TIME, getDeployTime());
      footPrintData.put(CollectionConstants.FOOTPRINT_VC_CPU_QUOTA_SIZE, getCpuQuotaSizeOfVC());
      footPrintData.put(CollectionConstants.FOOTPRINT_VC_MEM_QUOTA_SIZE, getMemoryQuotaSizeOfVC());
      footPrintData.put(CollectionConstants.FOOTPRINT_RESOURCEPOOL_CPU_QUOTA_SIZE, getCpuQuotaSizeOfResourcePool());
      footPrintData.put(CollectionConstants.FOOTPRINT_RESOURCEPOOL_MEM_QUOTA_SIZE, getMemoryQuotaSizeOfResourcePool());
      footPrintData.put(CollectionConstants.FOOTPRINT_DATASTORE_QUOTA_SIZE, getDatastoreQuotaSizeInBDE());
      footPrintData.put(CollectionConstants.FOOTPRINT_IS_INIT_RESOURCE, Boolean.toString(isResourceInitialized()));
      footPrintData.put(CollectionConstants.FOOTPRINT_HOST_NUM_OF_VC, numberOfHostInVC());
      footPrintData.put(CollectionConstants.FOOTPRINT_HOST_NUM_OF_RESOURCE_POOLS, numberOfHostInResourcePools());
      footPrintData.put(CollectionConstants.FOOTPRINT_HOST_NUM_OF_CLUSTERS, numberOfHostInClusters());
      footPrintData.put(CollectionConstants.FOOTPRINT_NUM_OF_HADOOP_CLUSTERS, getNumberOfClusters());
      footPrintData.put(CollectionConstants.FOOTPRINT_NUM_OF_HADOOP_NODES, getNumberOfNodes());
      return footPrintData;
   }

   private Map<String, Object> getEnvironmentalInfo() {
      Map<String, Object> environmentalInfoData = new HashMap<String, Object>();
      environmentalInfoData.put(CollectionConstants.OBJECT_ID, CommonUtil.getUUID());
      environmentalInfoData.put(CollectionConstants.ENVIRONMENTAL_INFO_VERSION_OF_VCENTER, getVersionOfVCenter());
      environmentalInfoData.put(CollectionConstants.ENVIRONMENTAL_INFO_VERSION_OF_ESXI, getVersionsOfESXi());
      environmentalInfoData.put(CollectionConstants.ENVIRONMENTAL_INFO_TYPE_OF_STORAGE, typesOfStorages());
      environmentalInfoData.put(CollectionConstants.ENVIRONMENTAL_INFO_DISTROS_OF_HADOOP, getDistrosOfHadoop());
      environmentalInfoData.put(CollectionConstants.ENVIRONMENTAL_INFO_APP_MANAGERS, getAppManagers());
      return environmentalInfoData;
   }

   private Map<String, Object> getCommonReportsData() {
      Map<String, Object> commonReportsData = new HashMap<String, Object>();
      commonReportsData.put(CollectionConstants.OBJECT_ID, collectionInitializerService.getInstanceId());
      commonReportsData.put(CollectionConstants.PRODUCT_INSTANCE_NAME, CollectionConstants.PRODUCT_INSTANCE_NAME_VALUE);
      commonReportsData.put(CollectionConstants.PRODUCT_INSTANCE_VERSION, Constants.VERSION);
      commonReportsData.put(CollectionConstants.PRODUCT_INSTANCE_EDITION, CollectionConstants.PRODUCT_INSTANCE_EDITION_VALUE);
      return commonReportsData;
   }

   private String getBDEVersion() {
      return Constants.VERSION;
   }

   private boolean isResourceInitialized() {
      return resourceInitializerService.isResourceInitialized();
   }

   private String getDeployTime() {
      String deployTimeStr = "";
      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DEPLOY_TIME_FORMAT);
      Date deployTime = collectionInitializerService.getDeployTime();
      if (deployTime != null) {
         deployTimeStr = sdf.format(deployTime);
      }
      return deployTimeStr;
   }

   private int getNumberOfClusters() {
      int numberOfClusters = 0;
      List<ClusterRead> clusters = clusterMgr.getClusters(false);
      if (clusters != null && !clusters.isEmpty()) {
         numberOfClusters = clusters.size();
      }
      return numberOfClusters;
   }

   private int getNumberOfNodes() {
      int numberOfNodes = 0;
      List<ClusterRead> clusters = clusterMgr.getClusters(false);
      for (ClusterRead cluster : clusters) {
         if (cluster.getNodeGroups() != null
               && !cluster.getNodeGroups().isEmpty()) {
            for (NodeGroupRead nodeGroup : cluster.getNodeGroups()) {
               numberOfNodes += nodeGroup.getInstanceNum();
            }
         }
      }
      return numberOfNodes;
   }

   private long getDatastoreQuotaSizeInBDE() {
      long quotaSize = 0L;
      List<VcDatastore> vcDatastores = resourceService.getAvailableDSs();
      if (vcDatastores != null && !vcDatastores.isEmpty()) {
         for (VcDatastore vcDatastore : vcDatastores) {
            quotaSize += vcDatastore.getCapacity();
         }
      }
      if (quotaSize != 0) {
         quotaSize = quotaSize/1024/1024/1024;
      }
      return quotaSize;
   }

   private long getMemoryQuotaSizeOfVC() {
      long totalMemory = 0L;
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null) {
         VcDatacenter vcDatacenter = serverVm.getDatacenter();
         if (vcDatacenter != null) {
            List<VcCluster> vcClusters = vcDatacenter.getVcClusters();
            if (vcClusters != null && !vcClusters.isEmpty()) {
               for (VcCluster vcCluster : vcClusters) {
                  totalMemory += vcCluster.getTotalMemory();
               }
            }
         }
      }
      if (totalMemory != 0) {
         totalMemory = totalMemory >> 20;
      }
      return totalMemory;
   }

   private int getCpuQuotaSizeOfVC() {
      int totalCpu = 0;
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null) {
         VcDatacenter vcDatacenter = serverVm.getDatacenter();
         if (vcDatacenter != null) {
            List<VcCluster> vcClusters = vcDatacenter.getVcClusters();
            if (vcClusters != null && !vcClusters.isEmpty()) {
               for (VcCluster vcCluster : vcClusters) {
                  totalCpu += vcCluster.getTotalCpu();
               }
            }
         }
      }
      return totalCpu;
   }

   private long getCpuQuotaSizeOfResourcePool() {
      long cpuLimit = 0L;
      List<ResourcePoolRead> resourcePools =
            resourcePoolService.getAllResourcePoolForRest();
      if (resourcePools != null && !resourcePools.isEmpty()) {
         long cpuMaxLimit = 0L;
         List<VcResourcePool> rootVcResourcePools = findRootResourcePools(resourcePools);
         for (VcResourcePool vcResourcePool : rootVcResourcePools) {
            if (vcResourcePool !=null && vcResourcePool.getCpuAllocationInfo() != null) {
               cpuMaxLimit = vcResourcePool.getCpuAllocationInfo().getLimit();
               if (cpuMaxLimit == -1) {
                  if (vcResourcePool.getCpuUsageInfo() != null) {
                     cpuMaxLimit =
                           vcResourcePool.getCpuUsageInfo().getMaxUsage();
                  } else {
                     cpuMaxLimit = 0L;
                  }
               }
               cpuLimit += cpuMaxLimit;
            }
         }
      }
      return cpuLimit;
   }

   private long getMemoryQuotaSizeOfResourcePool() {
      long memoryLimit = 0L;
      List<ResourcePoolRead> resourcePools =
            resourcePoolService.getAllResourcePoolForRest();
      if (resourcePools != null && !resourcePools.isEmpty()) {
         long memoryMaxLimit = 0L;
         List<VcResourcePool> rootVcResourcePools = findRootResourcePools(resourcePools);
         for (VcResourcePool vcResourcePool : rootVcResourcePools) {
            if (vcResourcePool != null && vcResourcePool.getMemAllocationInfo() != null) {
               memoryMaxLimit = vcResourcePool.getMemAllocationInfo().getLimit();
               if (memoryMaxLimit == -1) {
                  if (vcResourcePool.getMemUsageInfo() != null) {
                     memoryMaxLimit = vcResourcePool.getMemUsageInfo().getMaxUsage();
                  } else {
                     memoryMaxLimit = 0L;
                  }
               }
               memoryLimit += memoryMaxLimit;
            }
         }
      }
      if (memoryLimit != 0) {
         memoryLimit = memoryLimit >> 20;
      }
      return memoryLimit;
   }

   private List<VcResourcePool> findRootResourcePools(
         List<ResourcePoolRead> resourcePools) {
      List<VcResourcePool> rootVcResourcePools = new ArrayList<>();
      List<VcResourcePool> vcResourcePools = new ArrayList<>();
      VcResourcePool vcResourcePool = null;
      for (ResourcePoolRead resourcePool : resourcePools) {
         vcResourcePool =
               VcResourceUtils.findRPInVCCluster(resourcePool.getVcCluster(),
                     resourcePool.getRpVsphereName());
         vcResourcePools.add(vcResourcePool);
      }
      int count;
      for (VcResourcePool vcRp : vcResourcePools) {
         if (vcRp.isRootRP()) {
            rootVcResourcePools.add(vcRp);
         } else {
            count = 0;
            for (VcResourcePool vcrp : vcResourcePools) {
               if (vcrp.equals(vcRp)) {
                  continue;
               }
               if (vcrp.getPath().startsWith(vcRp.getPath())) {
                  break;
               }
               count ++;
            }
            if (count == vcResourcePools.size()-1) {
               rootVcResourcePools.add(vcRp);
            }
         }
      }
      return rootVcResourcePools;
   }

   private int numberOfHostInVC() {
      int numberOfHost = 0;
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null) {
         VcDatacenter vcDatacenter = serverVm.getDatacenter();
         if (vcDatacenter != null) {
            List<VcCluster> vcClusters = vcDatacenter.getVcClusters();
            if (vcClusters != null && !vcClusters.isEmpty()) {
               for (VcCluster vcCluster : vcClusters) {
                  numberOfHost += vcCluster.getNumberOfHost();
               }
            }
         }
      }
      return numberOfHost;
   }

   private int numberOfHostInResourcePools() {
      int numberOfHost = 0;
      Set<String> rpNames = resourcePoolService.getAllRPNames();
      if (rpNames != null && !rpNames.isEmpty()) {
         Set<String> hostNames = new HashSet<> ();
         Iterator<String> it = rpNames.iterator();
         String rpName = "";
         while(it.hasNext()) {
            rpName = it.next();
            List<VcHost> vcHosts = resourceService.getHostsByRpName(rpName);
            for (VcHost vcHost : vcHosts) {
               hostNames.add(vcHost.getName());
            }
         }
         numberOfHost = hostNames.size();
      }
      return numberOfHost;
   }

   private int numberOfHostInClusters() {
      int numberOfHost = 0;
      List<ClusterRead> clusters = clusterMgr.getClusters(false);
      if (clusters != null && !clusters.isEmpty()) {
         List<NodeRead> nodes = null;
         VcVirtualMachine vm = null;
         VcHost host = null;
         Set<String> hostNames = new HashSet<>();
         for (ClusterRead cluster : clusters) {
            List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();
            if (nodeGroups != null && !nodeGroups.isEmpty()) {
               for (NodeGroupRead nodeGroup : nodeGroups) {
                  nodes = nodeGroup.getInstances();
                  if (nodes != null && !nodes.isEmpty()) {
                     for (NodeRead node : nodes) {
                        vm = VcCache.getIgnoreMissing(node.getMoId());
                        if (vm != null) {
                           host = vm.getHost();
                           if (host != null) {
                              hostNames.add(host.getName());
                           }
                        }
                     }
                  }
               }
            }
         }
         numberOfHost = hostNames.size();
      }
      return numberOfHost;
   }

   private String getVersionOfVCenter() {
      String version = "";
      VcContext.initVcContext();
      version = VcContext.getVcVersion();
      if (!CommonUtil.isBlank(version)) {
         version = "vCenter" + " " + version;
      }
      return version;
   }

   private String getVersionsOfESXi() {
      Map<String, Integer> versionCountMap = new HashMap <>();
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null) {
         VcDatacenter vcDatacenter = serverVm.getDatacenter();
         if (vcDatacenter != null) {
            List<VcCluster> vcClusters = vcDatacenter.getVcClusters();
            if (vcClusters != null && !vcClusters.isEmpty()) {
               int versionCount = 0;
               String esxiVersion = "";
               for (VcCluster vcCluster : vcClusters) {
                  List<VcHost> vcHosts =
                        VcResourceUtils.findAllHostsInVCCluster(vcCluster
                              .getName());
                  if (vcHosts != null) {
                     for (VcHost vcHost : vcHosts) {
                        esxiVersion = "ESXi" + " " + vcHost.getVersion();
                        if (!versionCountMap.containsKey(esxiVersion)) {
                           versionCountMap.put(esxiVersion, 1);
                        } else {
                           versionCount = versionCountMap.get(esxiVersion);
                           versionCountMap.put(esxiVersion, ++versionCount);
                        }
                     }
                  }
               }
            }
         }
      }
      String versions = CommonUtil.inputsConvert(versionCountMap);
      return versions;
   }

   private String typesOfStorages() {
      StringBuffer typesOfStoragesBuff = new StringBuffer();
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      int localTypeOfStorages = 0;
      int remoteTypeOfStorages = 0;
      if (serverMobId == null) {
         throw ClusteringServiceException.TEMPLATE_ID_NOT_FOUND();
      }
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      if (serverVm != null) {
         VcDatacenter vcDatacenter = serverVm.getDatacenter();
         if (vcDatacenter != null) {
            List<VcDatastore> datastores = null;
            List<VcHost> hosts = null;
            List<VcCluster> vcClusters = vcDatacenter.getVcClusters();
            if (vcClusters != null && !vcClusters.isEmpty()) {
               for (VcCluster vcCluster : vcClusters) {
                  datastores = vcCluster.getAllDatastores();
                  if (datastores != null && !datastores.isEmpty()) {
                     for (VcDatastore datastore : datastores) {
                        hosts = datastore.getHosts();
                        if (hosts != null && !hosts.isEmpty()) {
                           if (hosts.size() == 1) {
                              localTypeOfStorages++;
                           } else {
                              remoteTypeOfStorages++;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      typesOfStoragesBuff.append("LOCAL:").append(localTypeOfStorages)
            .append(",").append("REMOTE:").append(remoteTypeOfStorages);
      return typesOfStoragesBuff.toString();
   }

   private String getAppManagers() {
      List<AppManagerRead> appManagers =
            softwareManagerCollector.getAllAppManagerReads();
      Set<String> appManagerTypes = new HashSet<>();
      for (AppManagerRead appManager : appManagers) {
         appManagerTypes.add(appManager.getType());
      }
      return CommonUtil.inputsConvert(appManagerTypes);
   }

   private String getDistrosOfHadoop() {
      Set<String> distroNames = new HashSet<>();
      List<AppManagerRead> appManagers =
            softwareManagerCollector.getAllAppManagerReads();
      if (appManagers != null && !appManagers.isEmpty()) {
         SoftwareManager softwareManager = null;
         List<HadoopStack> hadoopStacks = null;
         for (AppManagerRead appManager : appManagers) {
            softwareManager =
                  softwareManagerCollector.getSoftwareManager(appManager
                        .getName());
            hadoopStacks = softwareManager.getSupportedStacks();
            if (hadoopStacks != null && !hadoopStacks.isEmpty()) {
               for (HadoopStack hadoopStack : hadoopStacks) {
                  distroNames.add(hadoopStack.getVendor() + " "
                        + hadoopStack.getFullVersion());
               }
            }
         }
      }
      return CommonUtil.inputsConvert(distroNames);
   }

   public ICollectionInitializerService getCollectionInitializerService() {
      return collectionInitializerService;
   }

   public void setCollectionInitializerService(
         ICollectionInitializerService collectionInitializerService) {
      this.collectionInitializerService = collectionInitializerService;
   }

}
