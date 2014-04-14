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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.exception.ClusterConfigException;

public class ClusterCreateTest {

   @Test
   public void testSupportedWithHdfs2() {
      ClusterCreate cluster = new ClusterCreate();
      cluster.setDistroVendor(Constants.DEFAULT_VENDOR);
      cluster.setDistroVersion("1.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVendor(Constants.CDH_VENDOR);
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
      cluster.setDistroVendor(Constants.GPHD_VENDOR);
      cluster.setDistroVersion("1.2");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setDistroVendor(Constants.MAPR_VENDOR);
      cluster.setDistroVersion("2.0");
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
      assertEquals("Invalid storage type NFS. "
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
      assertEquals("Duplicate NameNode or DataNode role.", failedMsgList.get(0));
      assertEquals("Missing JobTracker or ResourceManager role.",
            failedMsgList.get(1));
      failedMsgList.clear();
      cluster.setExternalHDFS("");
      cluster.setNodeGroups(new NodeGroupCreate[] { compute });
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(1, failedMsgList.size());
      assertEquals("Cannot find one or more roles in " + ServiceType.MAPRED + " "
            + ServiceType.MAPRED.getRoles()
            + " in the cluster specification file.", failedMsgList.get(0));
      failedMsgList.clear();
      NodeGroupCreate master = new NodeGroupCreate();
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_JOBTRACKER_ROLE
            .toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { master, compute });
      assertEquals(false, cluster.validateNodeGroupRoles(failedMsgList));
      assertEquals(1, failedMsgList.size());
      assertEquals("Some dependent services " + EnumSet.of(ServiceType.HDFS)
            + " " + ServiceType.MAPRED
            + " relies on cannot be found in the spec file.",
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

      Map<NetTrafficType, List<String>> networkConfig = new HashMap<NetTrafficType, List<String>>();

      List<String> mgtnets = new ArrayList<String>();
      mgtnets.add("nw1");
      networkConfig.put(NetTrafficType.MGT_NETWORK, mgtnets);

      List<String> hadpnets = new ArrayList<String>();
      hadpnets.add("nw2");
      networkConfig.put(NetTrafficType.HDFS_NETWORK, hadpnets);

      cluster.setNetworkConfig(networkConfig);

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
      cluster.setNodeGroups(new NodeGroupCreate[] { master, worker, client });
      cluster.validateClusterCreate(failedMsgList, warningMsgList);
      assertEquals(2, failedMsgList.size());
      assertEquals("The 'swapRatio' must be greater than 0 in group master.",
            failedMsgList.get(0));
      assertEquals("worker.instanceNum=0.", failedMsgList.get(1));
      assertEquals(1, warningMsgList.size());
      assertEquals(
            "Warning: The size of the virtual machine memory must be evenly divisible by 4. For group master, 7500 replaces 7501 for the memCapacityMB value.",
            warningMsgList.get(0));
   }

   @Test
   public void testValidateNodeGroupNames() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate worker1 = new NodeGroupCreate();
      worker1.setName("test-1");
      cluster.setNodeGroups(new NodeGroupCreate[] { worker1 });
      try {
         cluster.validateNodeGroupNames();
      } catch (ClusterConfigException e) {
         assertEquals(
               "Invalid node group name 'test-1'. Revise the cluster specification file.",
               e.getMessage());
      }
      NodeGroupCreate worker2 = new NodeGroupCreate();
      worker2
            .setName("test12345678901234567890123456789012345678901234567890123456789012345678901234567890");
      cluster.setNodeGroups(new NodeGroupCreate[] { worker2 });
      try {
         cluster.validateNodeGroupNames();
      } catch (ClusterConfigException e) {
         assertEquals(
               "Invalid node group name 'test12345678901234567890123456789012345678901234567890123456789012345678901234567890'. Revise the cluster specification file.",
               e.getMessage());
      }
      NodeGroupCreate worker3 = new NodeGroupCreate();
      worker3.setName("");
      cluster.setNodeGroups(new NodeGroupCreate[] { worker3 });
      try {
         cluster.validateNodeGroupNames();
      } catch (ClusterConfigException e) {
         assertEquals(
               "Invalid node group name ''. Revise the cluster specification file.",
               e.getMessage());
      }
      NodeGroupCreate worker4 = new NodeGroupCreate();
      worker4.setName("test4");
      cluster.setNodeGroups(new NodeGroupCreate[] { worker4 });
      cluster.validateNodeGroupNames();
   }

   @Test
   public void testVerifyClusterNameLength() {
      boolean failed = false;
      ClusterCreate cluster = new ClusterCreate();
      cluster.setName("Test");
      cluster.verifyClusterNameLength();
      ConfigInfo.setSerengetiUUID("Serengeti.uuid");
      cluster
            .setName("Test1234567890123456789012345678901234567890123456789012345678901234");
      try {
         cluster.verifyClusterNameLength();
      } catch (ClusterConfigException e) {
         assertEquals(e.getMessage(),
               "The length of the cluster name must be equal or less than 65.");
         failed = true;
      }
      assertTrue(failed);
      failed = false;
      cluster
            .setName("Test1234567890123456789012345678901234567890123456789012345678901234");
      NodeGroupCreate resourceManager = new NodeGroupCreate();
      resourceManager.setName("resourceManager");
      NodeGroupCreate zookeeper = new NodeGroupCreate();
      zookeeper.setName("zookeeper");
      cluster
            .setNodeGroups(new NodeGroupCreate[] { resourceManager, zookeeper });
      try {
         cluster.verifyClusterNameLength();
      } catch (ClusterConfigException e) {
         assertEquals(e.getMessage(),
               "The length of the cluster name must be equal or less than 59.");
         failed = true;
      }
      assertTrue(failed);
      failed = false;
      NodeGroupCreate otherNodeGroup = new NodeGroupCreate();
      otherNodeGroup
            .setName("NodeGroup12345678901234567890123456789012345678901234567890123456789012345");
      cluster.setNodeGroups(new NodeGroupCreate[] { otherNodeGroup });
      try {
         cluster.verifyClusterNameLength();
      } catch (ClusterConfigException e) {
         assertEquals(e.getMessage(),
               "The length of the node group name must be less than 74.");
         failed = true;
      }
      assertTrue(failed);
   }

   @Test
   public void testVerifyClusterNameLengthWhenUUIDTooLong() {
      boolean failed = false;
      ClusterCreate cluster = new ClusterCreate();
      cluster.setName("Test");
      ConfigInfo
            .setSerengetiUUID("Test123456789012345678901234567890123456789012345678901234567890123456789012345");
      try {
         cluster.verifyClusterNameLength();
      } catch (ClusterConfigException e) {
         failed = true;
         assertEquals(e.getMessage(),
               "The length of the UUID must be less than 79.");
      }
      assertTrue(failed);
   }
}
