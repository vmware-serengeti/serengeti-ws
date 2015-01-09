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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vmware.bdd.apitypes.*;
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
   private IClusterEntityManager clusterEntityMgr;
   @Autowired
   protected SoftwareManagerCollector softwareManagerCollector;
   @Autowired
   private INetworkService networkService;

   @Override
   public Map<String, Map<String, Object>> collectData(Map<String, Object> rawdata,
         DataObjectType operation) {
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
      String operationName = (String) rawdata.get("operation_name");
      Map<String, Map<Class, Object>> operationParameters =
              (Map<String, Map<Class, Object>>) rawdata.get("operation_parameters");
      if (!CommonUtil.isBlank(operationName) && !operationParameters.isEmpty()) {
          String clusterName = "";
          Map<Class, Object> args = operationParameters.get("arg0");
          if (args != null) {
              for (Entry<Class, Object> arg : args.entrySet()) {
                  switch (operationName.trim()) {
                      case "createCluster" :
                          ClusterCreate spec = (ClusterCreate) arg.getValue();
                          clusterName = spec.getName();
                          break;
                      case "scaleNodeGroupResource" :
                          ResourceScale scale = (ResourceScale) arg.getValue();
                          clusterName = scale.getClusterName();
                          break;
                      default:
                          clusterName = (String) arg.getValue();
                  }
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
              clusterSnapshotData.put("id", uuid);
              clusterSnapshotData.put("use_external_hdfs", usedExternalHDFS);
              clusterSnapshotData.put("hadoop_ecosystem_information",
                      hadoopEcosystemInformation);
              clusterSnapshotData.put("distro", clusterEntity.getDistro());
              clusterSnapshotData.put("distro_version",
                      clusterEntity.getDistroVersion());
              clusterSnapshotData.put("distro_vendor",
                      clusterEntity.getDistroVendor());
              clusterSnapshotData.put("type_of_network", typeOfNetwork);
              clusterSnapshotData.put("node_number", clusterRead.getInstanceNum());
              clusterSnapshotData.put("cpu_number", cpuNumber);
              clusterSnapshotData.put("memory_size", memorySize);
              clusterSnapshotData.put("datastore_size", datastoreSize);
              clusterSnapshotData.put("cluster_spec", getClusterSpec(clusterRead.getName()));
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
                   typeSet.add("STATIC IP");
               } else {
                   typeSet.add("DHCP");
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
               datastoreSize += storageRead.getSizeGB();
            }
         }
      }
      return datastoreSize;
   }

   private Long getTotalMemorySize(List<NodeGroupRead> nodeGroups) {
      long memorySize = 0L;
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : nodeGroups) {
            memorySize += nodeGroup.getMemCapacityMB();
         }
      }
      return memorySize;
   }

   private Integer getTotalCpuNumber(List<NodeGroupRead> nodeGroups) {
      int cpuNum = 0;
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : nodeGroups) {
            cpuNum += nodeGroup.getCpuNum();
         }
      }
      return cpuNum;
   }

   private String getHadoopEcosystemInformation(List<NodeGroupRead> nodeGroups) {
      Set<String> nameSet = new HashSet<String>();
      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : nodeGroups) {
            if (nodeGroup.getRoles().contains(HadoopRole.PIG_ROLE.toString())
                  || nodeGroup.getRoles().contains(HadoopRole.MAPR_PIG_ROLE)) {
               nameSet.add("Pig");
            }
            if (nodeGroup.getRoles().contains(HadoopRole.HIVE_ROLE.toString())
                  || nodeGroup.getRoles().contains(
                        HadoopRole.HIVE_SERVER_ROLE.toString())
                  || nodeGroup.getRoles().contains(
                        HadoopRole.MAPR_HIVE_ROLE.toString())
                  || nodeGroup.getRoles().contains(
                        HadoopRole.MAPR_HIVE_SERVER_ROLE.toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.HIVE_SERVER_ROLE
                                    .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.HIVE_METASTORE_ROLE
                                    .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.HIVE_METASTORE_ROLE
                                    .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.HIVE_SERVER2_ROLE
                                    .toString())) {
               nameSet.add("Hive");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.OOZIE_SERVER_ROLE
                              .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.OOZIE_SERVER_ROLE
                                    .toString())) {
               nameSet.add("Oozie");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.GANGLIA_SERVER_ROLE
                              .toString())) {
               nameSet.add("Ganglia");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.NAGIOS_SERVER_ROLE
                              .toString())) {
               nameSet.add("Nagios");
            }
            if (nodeGroup.getRoles().contains(
                  com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.NIMBUS_ROLE
                        .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.STORM_REST_API_ROLE
                                    .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.ambari.spectypes.HadoopRole.STORM_UI_SERVER_ROLE
                                    .toString())) {
               nameSet.add("Storm");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.IMPALA_CATALOG_SERVER_ROLE
                              .toString())) {
               nameSet.add("Impala");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SQOOP_SERVER_ROLE
                              .toString())) {
               nameSet.add("Sqoop");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SPARK_MASTER_ROLE
                              .toString())
                  || nodeGroup
                        .getRoles()
                        .contains(
                              com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SPARK_HISTORY_SERVER_ROLE
                                    .toString())) {
               nameSet.add("Spark");
            }
            if (nodeGroup
                  .getRoles()
                  .contains(
                        com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole.SOLR_SERVER_ROLE
                              .toString())) {
               nameSet.add("Solr");
            }
         }
      }
      return CommonUtil.inputsConvert(nameSet);
   }

   private Map<String, Map<String, Object>> packagingOperationData(
         Map<String, Map<String, Object>> data, Map<String, Object> rawdata) {
       Map<String, Object> modifydata = new HashMap<>();
       modifydata.putAll(rawdata);
       modifydata.remove("task_id");
       modifydata.put("id", CommonUtil.getUUID());
       Map<String, Map<Class, Object>> operationParameters =
               (Map<String, Map<Class, Object>>) rawdata.get("operation_parameters");
       if (!operationParameters.isEmpty()) {
           MethodParameter methodParameter = new MethodParameter();
           for (Entry<String, Map<Class, Object>> parameterEntry : operationParameters.entrySet()) {
               Map<Class, Object> parameter = parameterEntry.getValue();
               if (parameter != null) {
                   Iterator<Entry<Class, Object>> it = parameter.entrySet().iterator();
                   if (it.hasNext()) {
                       Entry<Class, Object> argV = it.next();
                       methodParameter.setParameter(parameterEntry.getKey(), argV.getValue());
                   }
               }
         }
           modifydata.put("operation_parameters", methodParameter);
      } else {
           modifydata.put("operation_parameters", "");
      }
       data.put(DataObjectType.OPERATION.getName(), modifydata);
       return data;
   }

   @Override
   public Map<String, Map<String, ?>> mergeData(
         Map<String, Map<String, Object>> operationData,
         Map<String, Map<String, Object>> clusterSnapshotData) {
      Map<String, Map<String, ?>> data = new HashMap<String, Map<String, ?>>();
      String clusterId =
              (String) clusterSnapshotData.get(DataObjectType.CLUSTER_SNAPSHOT.getName()).get("id");
      Map<String, Object> relatedObject = new HashMap<>();
      relatedObject.put("cluster_id", clusterId);
      operationData.get(DataObjectType.OPERATION.getName()).put("cluster_id", relatedObject);
      data.putAll(operationData);
      data.putAll(clusterSnapshotData);
      return data;
   }

}
