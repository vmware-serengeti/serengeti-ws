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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;

public class ClusterCreateTest {

   @Test
   public void testSupportedWithHdfs2() {
      ClusterCreate cluster = new ClusterCreate();
      cluster.setDistroVendor(Constants.DEFAULT_VENDOR);
      cluster.setDistroVersion("1.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVendor(Constants.CDH_VENDOR);
      cluster.setDistroVersion("1.0.0");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("3u3");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.1.2");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.1");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.2");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.1.0.2");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.2.0.1");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.15.0.1");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.100.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.1.0.2.3");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVersion("4.2.0.2.3");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVendor(Constants.PHD_VENDOR);
      assertEquals(true, cluster.supportedWithHdfs2());
   }

   @Test
   public void testGetDefaultDistroName() {
      ClusterCreate cluster = new ClusterCreate();
      DistroRead dr1 = new DistroRead();
      dr1.setVendor(Constants.CDH_VENDOR);
      dr1.setName("CDH");
      Assert.assertNull(cluster.getDefaultDistroName(new DistroRead[] { dr1 }));
      DistroRead dr2 = new DistroRead();
      dr2.setVendor(Constants.DEFAULT_VENDOR);
      dr2.setName("apache");
      assertEquals(dr2.getName(),
            cluster.getDefaultDistroName(new DistroRead[] { dr1, dr2 }));
   }

   @Test
   public void testValidateCDHVersion() {
      List<String> warningMsgList = new LinkedList<String>();
      ClusterCreate cluster = new ClusterCreate();
      cluster.setDistroVendor(Constants.CDH_VENDOR);
      cluster.setDistroVersion("4.2.1");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
      warningMsgList.clear();
      cluster.setDistroVersion("4.2.2");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 1);
      warningMsgList.clear();
      cluster.setDistroVersion("4.12.0");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 1);
      warningMsgList.clear();
      cluster.setDistroVersion("4.1.2");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
      warningMsgList.clear();
      cluster.setDistroVersion("3");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
      warningMsgList.clear();
      cluster.setDistroVersion("4");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
      warningMsgList.clear();
      cluster.setDistroVersion("4.2");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
      cluster.setDistroVersion("3u6");
      cluster.validateCDHVersion(warningMsgList);
      assertEquals(true, warningMsgList.size() == 0);
   }

   @Test
   public void testValidateNodeGroupPlacementPolicies() {
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      ClusterCreate cluster = new ClusterCreate();
      assertEquals(true, cluster.validateNodeGroupPlacementPolicies(
            failedMsgList, warningMsgList));
      NodeGroupCreate compute = new NodeGroupCreate();
      compute.setName("compute");
      compute.setInstanceNum(10);
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      NodeGroupCreate data = new NodeGroupCreate();
      data.setName("data");
      compute.setInstanceNum(4);
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      cluster.setTopologyPolicy(TopologyType.HVE);
      assertEquals(true, cluster.validateNodeGroupPlacementPolicies(
            failedMsgList, warningMsgList));
      PlacementPolicy computePlacementPolicy = new PlacementPolicy();
      computePlacementPolicy.setInstancePerHost(3);
      compute.setPlacementPolicies(computePlacementPolicy);
      PlacementPolicy dataPlacementPolicy = new PlacementPolicy();
      dataPlacementPolicy.setInstancePerHost(0);
      data.setPlacementPolicies(dataPlacementPolicy);
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(false, cluster.validateNodeGroupPlacementPolicies(
            failedMsgList, warningMsgList));
      assertEquals(2, failedMsgList.size());
      failedMsgList.clear();
      GroupAssociation groupAssociation = new GroupAssociation();
      groupAssociation.setType(GroupAssociationType.STRICT);
      groupAssociation.setReference("data");
      computePlacementPolicy.setGroupAssociations(Arrays
            .asList(groupAssociation));
      computePlacementPolicy.setInstancePerHost(2);
      compute.setPlacementPolicies(computePlacementPolicy);
      compute.setInstanceNum(4);
      dataPlacementPolicy.setInstancePerHost(2);
      data.setInstanceNum(2);
      data.setPlacementPolicies(dataPlacementPolicy);
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(false, cluster.validateNodeGroupPlacementPolicies(
            failedMsgList, warningMsgList));
      assertEquals(1, failedMsgList.size());
   }

   @Test
   public void testValidateStorageType() {
      List<String> failedMsgList = new ArrayList<String>();
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate compute = new NodeGroupCreate();
      compute.setName("compute");
      StorageRead computeStorageRead = new StorageRead();
      computeStorageRead.setType("NFS");
      compute.setStorage(computeStorageRead);
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      NodeGroupCreate data = new NodeGroupCreate();
      data.setName("data");
      StorageRead dataStorageRead = new StorageRead();
      dataStorageRead.setType(DatastoreType.LOCAL.toString());
      data.setStorage(dataStorageRead);
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      cluster.validateStorageType(failedMsgList);
      assertEquals(1, failedMsgList.size());
      assertEquals("Storage type NFS is not allowed. "
            + Constants.STORAGE_TYPE_ALLOWED, failedMsgList.get(0));
      failedMsgList.clear();
      computeStorageRead.setType(DatastoreType.TEMPFS.toString());
      compute.setStorage(computeStorageRead);
      PlacementPolicy computePlacementPolicy = new PlacementPolicy();
      GroupAssociation groupAssociation = new GroupAssociation();
      groupAssociation.setType(GroupAssociationType.WEAK);
      computePlacementPolicy.setGroupAssociations(Arrays
            .asList(groupAssociation));
      compute.setPlacementPolicies(computePlacementPolicy);
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      cluster.validateStorageType(failedMsgList);
      assertEquals(1, failedMsgList.size());
      assertEquals(Constants.TEMPFS_NOT_ALLOWED, failedMsgList.get(0));
   }

   @Test
   public void testValidateNodeGroupRoles() {
      ClusterCreate cluster = new ClusterCreate();
      List<String> failedMsgList = new ArrayList<String>();
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      NodeGroupCreate compute = new NodeGroupCreate();
      NodeGroupCreate data = new NodeGroupCreate();
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(2, failedMsgList.size());
      failedMsgList.clear();
      cluster.setExternalHDFS("hdfs://192.168.0.2:9000");
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(2, failedMsgList.size());
      assertEquals("redundant namenode/datanode role", failedMsgList.get(0));
      assertEquals("missing jobtracker/resourcemanager role",
            failedMsgList.get(1));
      failedMsgList.clear();
      cluster.setExternalHDFS("");
      cluster.setNodeGroups(new NodeGroupCreate[] { compute });
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(1, failedMsgList.size());
      assertEquals("some roles in " + ServiceType.MAPRED + " "
            + ServiceType.MAPRED.getRoles()
            + " cannot be found in the spec file", failedMsgList.get(0));
      failedMsgList.clear();
      NodeGroupCreate master = new NodeGroupCreate();
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_JOBTRACKER_ROLE
            .toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { master, compute });
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(1, failedMsgList.size());
      assertEquals("some dependent services " + EnumSet.of(ServiceType.HDFS)
            + " " + ServiceType.MAPRED
            + " relies on cannot be found in the spec file",
            failedMsgList.get(0));
   }

   @Test
   public void testHasHDFSUrlConfigured() {
      ClusterCreate cluster = new ClusterCreate();
      assertEquals(false, cluster.hasHDFSUrlConfigured());
      cluster.setExternalHDFS("hdfs://192.168.0.2:9000");
      assertEquals(true, cluster.hasHDFSUrlConfigured());
   }

   @Test
   public void testValidateHDFSUrl() {
      ClusterCreate cluster = new ClusterCreate();
      cluster.setExternalHDFS("hdfs://192.168.0.2:9000");
      assertEquals(true, cluster.validateHDFSUrl());
      cluster.setExternalHDFS("hdf://192.168.0.2");
      assertEquals(false, cluster.validateHDFSUrl());
      cluster.setExternalHDFS("192.168.0.2:9000");
      assertEquals(false, cluster.validateHDFSUrl());
   }

   @Test
   public void testTotalInstances() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate compute = new NodeGroupCreate();
      compute.setInstanceNum(20);
      NodeGroupCreate data = new NodeGroupCreate();
      data.setInstanceNum(10);
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(30, cluster.totalInstances());
   }

   @Test
   public void testGetNodeGroup() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate compute = new NodeGroupCreate();
      compute.setName("compute");
      NodeGroupCreate data = new NodeGroupCreate();
      data.setName("data");
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(compute, cluster.getNodeGroup("compute"));
   }

   @Test
   public void testContainsComputeOnlyNodeGroups() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate compute = new NodeGroupCreate();
      NodeGroupCreate data = new NodeGroupCreate();
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(true, cluster.containsComputeOnlyNodeGroups());
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.TEMPFS_CLIENT_ROLE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(true, cluster.containsComputeOnlyNodeGroups());
      NodeGroupCreate worker = new NodeGroupCreate();
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { worker });
      assertEquals(false, cluster.containsComputeOnlyNodeGroups());
   }

   @Test
   public void testValidateClusterCreate() {
      ClusterCreate cluster = new ClusterCreate();
      cluster.setDistroVendor(Constants.DEFAULT_VENDOR);
      NodeGroupCreate master = new NodeGroupCreate();
      master.setName("master");
      master.setMemCapacityMB(7501);
      master.setSwapRatio(0F);
      master.setInstanceNum(1);
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_NAMENODE_ROLE.toString(),
            HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()));
      NodeGroupCreate worker = new NodeGroupCreate();
      worker.setName("worker");
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString(),
            HadoopRole.HADOOP_TASKTRACKER.toString()));
      worker.setMemCapacityMB(3748);
      worker.setInstanceNum(0);
      NodeGroupCreate client = new NodeGroupCreate();
      client.setName("client");
      client.setMemCapacityMB(3748);
      client.setInstanceNum(0);
      client.setRoles(Arrays.asList(HadoopRole.HADOOP_CLIENT_ROLE.toString(),
            HadoopRole.HIVE_SERVER_ROLE.toString(),
            HadoopRole.HIVE_ROLE.toString()));
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      List<String> distroRoles =
            Arrays.asList("hadoop_namenode", "hadoop_jobtracker",
                  "hadoop_tasktracker", "hadoop_datanode", "hadoop_client");
      cluster.setNodeGroups(new NodeGroupCreate[] { master, worker, client });
      cluster.validateClusterCreate(failedMsgList, warningMsgList, distroRoles);
      assertEquals(3, failedMsgList.size());
      assertEquals("'swapRatio' must be greater than 0 in group master",
            failedMsgList.get(0));
      assertEquals("worker.instanceNum=0", failedMsgList.get(1));
      assertEquals("client.roles=\"hive_server,hive\"", failedMsgList.get(2));
      assertEquals(1, warningMsgList.size());
      assertEquals(
            "Warning: VM's memory must be divisible by 4. So, 'memCapacityMB' will be converted from 7501 to 7504 automaticlly in the master group.",
            warningMsgList.get(0));
      
      master.setMemCapacityMB(16);
      worker.setMemCapacityMB(1023);
      cluster.validateClusterCreate(failedMsgList, warningMsgList, distroRoles);
      assertEquals(7, failedMsgList.size());
      assertEquals("'memCapacityMB' cannot be less than 1024 in group master,worker in order for nodes to run normally",
            failedMsgList.get(4));
   }

}
