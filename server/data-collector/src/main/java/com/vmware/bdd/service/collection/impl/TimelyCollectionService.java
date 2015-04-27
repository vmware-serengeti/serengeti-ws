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

import java.util.*;
import java.util.Map.Entry;

import com.vmware.bdd.apitypes.*;
import com.vmware.bdd.util.collection.CollectionConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.collection.ITimelyCollectionService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.CommonUtil;

public class TimelyCollectionService implements ITimelyCollectionService {

    private static final Logger logger = Logger.getLogger(TimelyCollectionService.class);

    @Autowired
    protected ClusterManager clusterMgr;
    @Autowired
    protected IClusterEntityManager clusterEntityMgr;
    @Autowired
    protected SoftwareManagerCollector softwareManagerCollector;
    @Autowired
    protected INetworkService networkService;

    private static Map<String, List<String>> productRolesMap = new HashMap<>();

    static {
        productRolesMap = generateProductRolesMap();
    }

    private static Map<String, List<String>> generateProductRolesMap() {
        Map<String, List<String>> productRolesMap = new HashMap<>();
        String[] pigRoles = new String[] {
                HadoopRole.PIG_ROLE.toString(),
                HadoopRole.MAPR_PIG_ROLE.toString()
        };
        String[] hiveRoles = new String[] {
                HadoopRole.HIVE_ROLE.toString(),
                HadoopRole.HIVE_SERVER_ROLE.toString(),
                HadoopRole.MAPR_HIVE_ROLE.toString(),
                HadoopRole.MAPR_HIVE_SERVER_ROLE.toString(),
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.HIVE_SERVER_ROLE.toString(),
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.HIVE_METASTORE_ROLE.toString(),
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.HIVE_METASTORE_ROLE.toString(),
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.HIVE_SERVER2_ROLE.toString()
        };
        String[] oozieRoles = new String[] {
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.OOZIE_SERVER_ROLE.toString(),
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.OOZIE_SERVER_ROLE.toString()
        };
        String[] gangliaRoles = new String[] {
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.GANGLIA_SERVER_ROLE.toString()
        };
        String[] nagiosRoles = new String[] {
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.NAGIOS_SERVER_ROLE.toString()
        };
        String[] stormRoles = new String[] {
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.NIMBUS_ROLE.toString(),
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.STORM_REST_API_ROLE.toString(),
                com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.STORM_UI_SERVER_ROLE.toString()
        };
        String[] impalaRoles = new String[] {
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.IMPALA_CATALOG_SERVER_ROLE.toString()
        };
        String[] sqoopRoles = new String[] {
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SQOOP_SERVER_ROLE.toString()
        };
        String[] sparkRoles = new String[] {
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SPARK_MASTER_ROLE.toString(),
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SPARK_HISTORY_SERVER_ROLE.toString()
        };
        String[] solrRoles = new String[] {
                com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SOLR_SERVER_ROLE.toString()
        };
        productRolesMap.put(CollectionConstants.PIG, Arrays.asList(pigRoles));
        productRolesMap.put(CollectionConstants.HIVE, Arrays.asList(hiveRoles));
        productRolesMap.put(CollectionConstants.OOZIE, Arrays.asList(oozieRoles));
        productRolesMap.put(CollectionConstants.GANGLIA, Arrays.asList(gangliaRoles));
        productRolesMap.put(CollectionConstants.NAGIOS, Arrays.asList(nagiosRoles));
        productRolesMap.put(CollectionConstants.STORM, Arrays.asList(stormRoles));
        productRolesMap.put(CollectionConstants.IMPALA, Arrays.asList(impalaRoles));
        productRolesMap.put(CollectionConstants.SQOOP, Arrays.asList(sqoopRoles));
        productRolesMap.put(CollectionConstants.SPARK, Arrays.asList(sparkRoles));
        productRolesMap.put(CollectionConstants.SOLR, Arrays.asList(solrRoles));
        return productRolesMap;
    }

    @Override
    public Map<String, Map<String, Object>> collectData(
            Map<String, Object> rawdata, DataObjectType operation) {
        Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();
        switch (operation) {
            case OPERATION :
                return packagingOperationData(data, rawdata);
            case CLUSTER_SNAPSHOT:
                return collectClusterSnapshotData(data, rawdata);
            default :
                return null;
        }
    }

   private Map<String, Map<String, Object>> collectClusterSnapshotData(
         Map<String, Map<String, Object>> data, Map<String, Object> rawdata) {
      String operationName = (String) rawdata.get(CollectionConstants.OPERATION_NAME);
       List<Object> operationParameters = (List<Object>) rawdata.get(CollectionConstants.OPERATION_PARAMETERS);
      if (!CommonUtil.isBlank(operationName) && !operationParameters.isEmpty()) {
          String clusterName = "";
          Object arg = operationParameters.get(0);
          if (arg != null) {
              switch (operationName.trim()) {
                  case CollectionConstants.METHOD_CREATE_CLUSTER :
                      ClusterCreate spec = (ClusterCreate) arg;
                      clusterName = spec.getName();
                      break;
                  case CollectionConstants.METHOD_SCALE_NODE_GROUP_RESOURCE :
                      ResourceScale scale = (ResourceScale) arg;
                      clusterName = scale.getClusterName();
                      break;
                  default:
                      clusterName = (String) arg;
                  }
          }
          if (!CommonUtil.isBlank(clusterName)) {
              Map<String, Object> clusterSnapshotData = new HashMap<>();
              ClusterEntity clusterEntity = clusterEntityMgr.findByName(clusterName);
              ClusterRead clusterRead =
                      clusterMgr.getClusterByName(clusterName, false);
              String uuid = CommonUtil.getUUID();
              String usedExternalHDFS =
                      CommonUtil.isBlank(clusterRead.getExternalHDFS()) ? "No" : "Yes";
              String hadoopEcosystemInformation =
                      getHadoopEcosystemInformation(clusterRead.getNodeGroups());
              String typeOfNetwork =
                      getTypeOfNetwork(clusterEntity.fetchNetworkNameList());
              Integer cpuNumber = getTotalCpuNumber(clusterRead.getNodeGroups());
              Long memorySize = getTotalMemorySize(clusterRead.getNodeGroups());
              Long datastoreSize =
                      getTotalDatastoreSize(clusterRead.getNodeGroups());
              clusterSnapshotData.put(CollectionConstants.OBJECT_ID, uuid);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_USE_EXTERNAL_HDFS, usedExternalHDFS);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_HADOOP_ECOSYSTEM_INFORMATION,
                      hadoopEcosystemInformation);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO, clusterEntity.getDistro());
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO_VERSION,
                      clusterEntity.getDistroVersion());
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO_VENDOR,
                      clusterEntity.getDistroVendor());
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_TYPE_OF_NETWORK, typeOfNetwork);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_NODE_NUMBER, clusterRead.getInstanceNum());
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_CPU_NUMBER, cpuNumber);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_MEMORY_SIZE, memorySize);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_DATASTORE_SIZE, datastoreSize);
              clusterSnapshotData.put(CollectionConstants.CLUSTER_SNAPSHOT_CLUSTER_SPEC, getClusterSpec(clusterRead.getName()));
              data.put(DataObjectType.CLUSTER_SNAPSHOT.getName(), clusterSnapshotData);
          }

      }
      return data;
   }

   private String getTypeOfNetwork(List<String> networkNames) {
       String types = "";
       Set<String> typeSet = new HashSet<>();
       NetworkRead network = null;
       for (String networkName : networkNames) {
           network =
                   networkService.getNetworkByName(networkName, false);
           if (network != null) {
               if (!network.isDhcp()) {
                   typeSet.add(CollectionConstants.STATIC_IP);
               } else {
                   typeSet.add(CollectionConstants.DHCP);
               }
           }
       }
       if (!typeSet.isEmpty()) {
           types = CommonUtil.inputsConvert(typeSet);
       }
       return types;
   }

   private ClusterCreate getClusterSpec(String clusterName) {
      ClusterCreate clusterSpec = clusterMgr.getClusterSpec(clusterName);
      return clusterSpec;
   }

   private Long getTotalDatastoreSize(List<NodeGroupRead> nodeGroups) {
      long datastoreSize = 0L;
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         StorageRead storageRead = null;
         for (NodeGroupRead nodeGroup : nodeGroups) {
            storageRead = nodeGroup.getStorage();
            if (storageRead != null) {
               datastoreSize += (storageRead.getSizeGB() * nodeGroup.getInstanceNum());
            }
         }
      }
      return datastoreSize;
   }

   private Long getTotalMemorySize(List<NodeGroupRead> nodeGroups) {
      long memorySize = 0L;
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : nodeGroups) {
            memorySize += (nodeGroup.getMemCapacityMB() * nodeGroup.getInstanceNum());
         }
      }
      return memorySize;
   }

   private Integer getTotalCpuNumber(List<NodeGroupRead> nodeGroups) {
      int cpuNum = 0;
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : nodeGroups) {
            cpuNum += (nodeGroup.getCpuNum() * nodeGroup.getInstanceNum());
         }
      }
      return cpuNum;
   }

   private String getHadoopEcosystemInformation(List<NodeGroupRead> nodeGroups) {
      Set<String> nameSet = new HashSet<String>();
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
          List<String> groupRoles = null;
          String product = "";
          List<String> roles = null;
          for (NodeGroupRead nodeGroup : nodeGroups) {
              groupRoles = nodeGroup.getRoles();
              for (Entry<String, List<String>> entry : productRolesMap.entrySet()) {
                  roles = entry.getValue();
                  for (String role : groupRoles) {
                      if (roles.contains(role)) {
                          product = entry.getKey();
                          nameSet.add(product);
                      }
                  }
              }
          }
      }
      return CommonUtil.inputsConvert(nameSet);
   }

   private Map<String, Map<String, Object>> packagingOperationData(
         Map<String, Map<String, Object>> data, Map<String, Object> rawdata) {
       Map<String, Object> modifydata = new HashMap<>();
       modifydata.putAll(rawdata);
       modifydata.remove(CollectionConstants.TASK_ID);
       modifydata.put(CollectionConstants.OBJECT_ID, CommonUtil.getUUID());
       List<Object> operationParameters =
               (List<Object>) rawdata.get(CollectionConstants.OPERATION_PARAMETERS);
       if (!operationParameters.isEmpty()) {
           MethodParameter methodParameter = new MethodParameter();
           int index = 0;
           for (Object parameter : operationParameters) {
               if (parameter != null) {
                   filterSensitiveData(parameter);
                   methodParameter.setParameter("arg" + index, parameter);
                   index ++;
               }
           }
           modifydata.put(CollectionConstants.OPERATION_PARAMETERS, methodParameter);
       } else {
           modifydata.put(CollectionConstants.OPERATION_PARAMETERS, "");
      }
       data.put(DataObjectType.OPERATION.getName(), modifydata);
       return data;
   }

    private void filterSensitiveData(Object parameter) {
        String className = parameter.getClass().getName();
        switch (className) {
            case "com.vmware.bdd.apitypes.NetworkAdd":
                NetworkAdd network = (NetworkAdd) parameter;
                network.setPortGroup(null);
                network.setDns1(null);
                network.setDns2(null);
                network.setGateway(null);
                network.setNetmask(null);
                break;
            case "com.vmware.bdd.apitypes.AppManagerAdd":
                AppManagerAdd appManager = (AppManagerAdd) parameter;
                appManager.setUsername(null);
                appManager.setPassword(null);
                appManager.setSslCertificate(null);
                break;
            case "com.vmware.bdd.apitypes.UserMgmtServer":
                UserMgmtServer userMgmtServer = (UserMgmtServer) parameter;
                userMgmtServer.setUserName(null);
                userMgmtServer.setPassword(null);
                break;
            case "com.vmware.bdd.apitypes.ClusterCreate":
                ClusterCreate cluster = (ClusterCreate) parameter;
                cluster.setHostnamePrefix(null);
                break;
            default:
        }
    }

    @Override
   public Map<String, Map<String, ?>> mergeData(
         Map<String, Map<String, Object>> operationData,
         Map<String, Map<String, Object>> clusterSnapshotData) {
      Map<String, Map<String, ?>> data = new HashMap<String, Map<String, ?>>();
      String clusterId = (String) clusterSnapshotData
              .get(DataObjectType.CLUSTER_SNAPSHOT.getName()).get(CollectionConstants.OBJECT_ID);
      Map<String, Object> relatedObject = new HashMap<>();
      relatedObject.put(CollectionConstants.CLUSTER_ID, clusterId);
      operationData.get(DataObjectType.OPERATION.getName()).put(CollectionConstants.CLUSTER_ID, relatedObject);
      data.putAll(operationData);
      data.putAll(clusterSnapshotData);
      return data;
   }

}
