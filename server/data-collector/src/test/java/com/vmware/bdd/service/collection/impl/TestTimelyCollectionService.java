/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.collection.impl;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.*;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.util.collection.CollectionConstants;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class TestTimelyCollectionService {

    @Test
    public void testCollectData() {
        testOperation(getRowData());
        testClusterSnapshot(getRowData());
    }

    private Map<String, Object> getRowData() {
        Map<String, Object> rawdata = new HashMap<>();
        int id = 123;
        rawdata.put(CollectionConstants.OBJECT_ID, CollectionConstants.SYNCHRONIZATION_PREFIX + id);
        rawdata.put(CollectionConstants.TASK_ID, id);
        rawdata.put(CollectionConstants.OPERATION_NAME, CollectionConstants.METHOD_CREATE_CLUSTER);
        long beginTime = System.currentTimeMillis();
        rawdata.put(CollectionConstants.OPERATION_BEGIN_TIME, beginTime);
        long endTime = System.currentTimeMillis();
        rawdata.put(CollectionConstants.OPERATION_END_TIME, endTime);
        rawdata.put(CollectionConstants.OPERATION_STATUS, ExitStatus.COMPLETED.getExitCode());
        rawdata.put(CollectionConstants.OPERATION_END_TIME, endTime);
        List<Object> parameters = new ArrayList<>();
        parameters.add(getClusterCreate());
        rawdata.put(CollectionConstants.OPERATION_PARAMETERS, parameters);
        return rawdata;
    }

    private void testClusterSnapshot(Map<String, Object> rawdata) {
        FakeTimelyCollectionService timelyCollectionService = new FakeTimelyCollectionService();
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        ClusterRead clusterRead = getClusterRead();
        clusterRead.setExternalHDFS("hdfs://168.192.0.71:8020");
        Mockito.when(clusterManager.getClusterSpec(Mockito.anyString())).thenReturn(getClusterCreate());
                Mockito.when(clusterManager.getClusterByName(Mockito.anyString(),
                        Mockito.anyBoolean())).thenReturn(clusterRead);
        IClusterEntityManager clusterEntityManager = Mockito.mock(IClusterEntityManager.class);
        ClusterEntity clusterEntity = new ClusterEntity("test");
        clusterEntity.setNetworkConfig("{\"MGT_NETWORK\":[{\"dns_type\":\"NORMAL\",\"is_generate_hostname\":false," +
                "\"hostname_prefix\":\"bde-74300b-\",\"port_group_name\":\"wdc-vhadp-pub3-10g\",\"n\n" +
                "etwork_name\":\"defaultNetwork\",\"traffic_type\":\"MGT_NETWORK\"}]}");
        clusterEntity.setDistro("CDH5");
        clusterEntity.setDistroVendor("CDH");
        clusterEntity.setDistroVersion("5.5");
        Mockito.when(clusterEntityManager.findByName(Mockito.anyString())).thenReturn(clusterEntity);
        SoftwareManagerCollector softwareManagerCollector = Mockito.mock(SoftwareManagerCollector.class);
        INetworkService networkService= Mockito.mock(INetworkService.class);
        NetworkRead networkRead = new NetworkRead();
        networkRead.setDhcp(true);
        Mockito.when(networkService.getNetworkByName(Mockito.anyString(),
                Mockito.anyBoolean())).thenReturn(networkRead);
        clusterManager.setClusterEntityMgr(clusterEntityManager);
        timelyCollectionService.setClusterMgr(clusterManager);
        timelyCollectionService.setClusterEntityMgr(clusterEntityManager);
        timelyCollectionService.setNetworkService(networkService);
        timelyCollectionService.setSoftwareManagerCollector(softwareManagerCollector);
        clusterManager.setClusterEntityMgr(clusterEntityManager);

        Map < String, Map < String, Object >> data =
                timelyCollectionService.collectData(rawdata, DataObjectType.CLUSTER_SNAPSHOT);
        assertEquals(data.size(), 1);
        Map<String, Object> clusterSnapshot = data.get(DataObjectType.CLUSTER_SNAPSHOT.getName());
        assertTrue(clusterSnapshot != null && !clusterSnapshot.isEmpty());
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_DATASTORE_SIZE), 250l);
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_HADOOP_ECOSYSTEM_INFORMATION), "Pig,Hive");
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_MEMORY_SIZE), 61000l);
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_NODE_NUMBER), 5);
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_USE_EXTERNAL_HDFS), "Yes");
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO_VENDOR), "CDH");
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO_VERSION), "5.5");
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_TYPE_OF_NETWORK), "DHCP");
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_CPU_NUMBER), 9);
        assertEquals(clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_DISTRO), "CDH5");
        ClusterCreate cluster = (ClusterCreate)clusterSnapshot.get(CollectionConstants.CLUSTER_SNAPSHOT_CLUSTER_SPEC);
        assertEquals(cluster.getName(), "test");
        assertEquals(cluster.getAppManager(), "Default");
        NodeGroupCreate[] nodeGroups = cluster.getNodeGroups();
        assertEquals(nodeGroups[0].getName(), "master");
        assertEquals(nodeGroups[1].getName(), "worker");
        assertEquals(nodeGroups[2].getName(), "client");
        assertEquals(nodeGroups[0].getInstanceNum(), 1);
        assertEquals(nodeGroups[0].getCpuNum().intValue(), 2);
        assertEquals(nodeGroups[0].getMemCapacityMB().longValue(), 12000l);
    }

    private void testOperation(Map<String, Object> rawdata) {
        TimelyCollectionService timelyCollectionService = new TimelyCollectionService();
        Map<String, Map<String, Object>> data = timelyCollectionService.collectData(rawdata, DataObjectType.OPERATION);
        assertTrue(data.containsKey(DataObjectType.OPERATION.getName()));
        Map<String, Object> operation = data.get(DataObjectType.OPERATION.getName());
        String objectId = (String)operation.get(CollectionConstants.OBJECT_ID);
        assertFalse(objectId.contains(CollectionConstants.SYNCHRONIZATION_PREFIX));
        MethodParameter methodParameter = (MethodParameter)operation.get(CollectionConstants.OPERATION_PARAMETERS);
        ClusterCreate cluster = (ClusterCreate) methodParameter.getParam().get("arg0");
        assertEquals(cluster.getName(), "test");
        assertEquals(cluster.getAppManager(), "Default");
        assertEquals(cluster.getNodeGroups().length, 3);
        for (NodeGroupCreate nodeGroupCreate : cluster.getNodeGroups()) {
            switch (nodeGroupCreate.getName()) {
                case "master":
                    assertEquals(nodeGroupCreate.getCpuNum().intValue(), 2);
                    assertEquals(nodeGroupCreate.getHaFlag(), "on");
                    assertEquals(nodeGroupCreate.getInstanceNum(), 1);
                    assertEquals(nodeGroupCreate.getMemCapacityMB().longValue(), 12000);
                    assertEquals(nodeGroupCreate.getMemCapacityMB().longValue(), 12000);
                    break;
                case "worker":
                    assertEquals(nodeGroupCreate.getCpuNum().intValue(), 2);
                    assertEquals(nodeGroupCreate.getHaFlag(), "off");
                    assertEquals(nodeGroupCreate.getInstanceNum(), 3);
                    assertEquals(nodeGroupCreate.getMemCapacityMB().longValue(), 12000);
                    break;
                case "client":
                    assertEquals(nodeGroupCreate.getCpuNum().intValue(), 1);
                    assertEquals(nodeGroupCreate.getHaFlag(), "off");
                    assertEquals(nodeGroupCreate.getInstanceNum(), 1);
                    assertEquals(nodeGroupCreate.getMemCapacityMB().longValue(), 13000);
                    break;
            }
        }
    }

    private ClusterRead getClusterRead() {
        ClusterRead clusterRead = new ClusterRead();
        clusterRead.setName("test");
        clusterRead.setAppManager("Default");
        clusterRead.setInstanceNum(5);

        NodeGroupRead master = new NodeGroupRead();
        List<String> rolesOfMaster = new ArrayList<>();
        StorageRead storageOfMaster = new StorageRead();
        storageOfMaster.setType("SHARED");
        storageOfMaster.setSizeGB(50);
        master.setName("master");
        master.setCpuNum(2);
        master.setInstanceNum(1);
        master.setMemCapacityMB(12000);
        master.setStorage(storageOfMaster);
        rolesOfMaster.add(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString());
        rolesOfMaster.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
        master.setRoles(rolesOfMaster);

        NodeGroupRead worker = new NodeGroupRead();
        List<String> rolesOfWorker = new ArrayList<>();
        StorageRead storageOfWorker = new StorageRead();
        storageOfWorker.setType("SHARED");
        storageOfWorker.setSizeGB(50);
        worker.setName("worker");
        worker.setCpuNum(2);
        worker.setInstanceNum(3);
        worker.setMemCapacityMB(12000);
        rolesOfWorker.add(HadoopRole.HADOOP_TASKTRACKER.toString());
        rolesOfWorker.add(HadoopRole.HADOOP_DATANODE.toString());
        worker.setRoles(rolesOfWorker);
        worker.setStorage(storageOfWorker);

        NodeGroupRead client = new NodeGroupRead();
        List<String> rolesOfClient = new ArrayList<>();
        StorageRead storageOfClient = new StorageRead();
        storageOfClient.setType("SHARED");
        storageOfClient.setSizeGB(50);
        client.setName("client");
        client.setCpuNum(1);
        client.setInstanceNum(1);
        client.setMemCapacityMB(13000);
        rolesOfClient.add(HadoopRole.HADOOP_TASKTRACKER.toString());
        rolesOfClient.add(HadoopRole.HADOOP_DATANODE.toString());
        rolesOfClient.add(HadoopRole.HIVE_ROLE.toString());
        rolesOfClient.add(HadoopRole.PIG_ROLE.toString());
        client.setRoles(rolesOfClient);
        client.setStorage(storageOfWorker);
        clusterRead.setNodeGroups(Arrays.asList(master, worker, client));
        return clusterRead;
    }

    private ClusterCreate getClusterCreate() {
        ClusterCreate clusterSpec = new ClusterCreate();
        clusterSpec.setName("test");
        clusterSpec.setAppManager("Default");
        NodeGroupCreate master = new NodeGroupCreate();
        List<String> rolesOfMaster = new ArrayList<>();
        StorageRead storageOfMaster = new StorageRead();
        storageOfMaster.setType("SHARED");
        storageOfMaster.setSizeGB(50);
        master.setName("master");
        master.setCpuNum(2);
        master.setHaFlag("on");
        master.setInstanceNum(1);
        master.setMemCapacityMB(12000);
        master.setStorage(storageOfMaster);
        rolesOfMaster.add(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString());
        rolesOfMaster.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
        master.setRoles(rolesOfMaster);
        String gConfigJson = "{\"configuration\":{}}";
        Map gConfig = (new Gson()).fromJson(gConfigJson, Map.class);
        master.setConfiguration(gConfig);

        NodeGroupCreate worker = new NodeGroupCreate();
        List<String> rolesOfWorker = new ArrayList<>();
        StorageRead storageOfWorker = new StorageRead();
        storageOfWorker.setType("SHARED");
        storageOfWorker.setSizeGB(50);
        worker.setName("worker");
        worker.setCpuNum(2);
        worker.setHaFlag("off");
        worker.setInstanceNum(3);
        worker.setMemCapacityMB(12000);
        rolesOfWorker.add(HadoopRole.HADOOP_TASKTRACKER.toString());
        rolesOfWorker.add(HadoopRole.HADOOP_DATANODE.toString());
        worker.setRoles(rolesOfWorker);
        worker.setStorage(storageOfWorker);

        NodeGroupCreate client = new NodeGroupCreate();
        List<String> rolesOfClient = new ArrayList<>();
        StorageRead storageOfClient = new StorageRead();
        storageOfClient.setType("SHARED");
        storageOfClient.setSizeGB(50);
        client.setName("client");
        client.setCpuNum(1);
        client.setHaFlag("off");
        client.setInstanceNum(1);
        client.setMemCapacityMB(13000);
        rolesOfClient.add(HadoopRole.HADOOP_TASKTRACKER.toString());
        rolesOfClient.add(HadoopRole.HADOOP_DATANODE.toString());
        client.setRoles(rolesOfClient);
        client.setStorage(storageOfWorker);

        clusterSpec.setNodeGroups(new NodeGroupCreate[]{master, worker, client});
        String configJson = "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                + "hdfs://168.192.0.71:8020" + "\"}}}}";
        Map config = (new Gson()).fromJson(configJson, Map.class);
        clusterSpec.setConfiguration((Map<String, Object>)(config.get("configuration")));
        return clusterSpec;
    }
}
