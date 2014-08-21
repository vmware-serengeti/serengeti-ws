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
package com.vmware.bdd.software.mgmt.plugin.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.plugin.ironfan.impl.ClusterValidator;
import com.vmware.bdd.plugin.ironfan.impl.DefaultSoftwareManagerImpl;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.utils.Constants;

public class DefaultSoftwareManagerImplTest {
   private static DefaultSoftwareManagerImpl defaultSoftwareManager = new DefaultSoftwareManagerImpl();
   private static ClusterValidator validator = new ClusterValidator();

   @BeforeClass
   public void setup() throws Exception {

   }

   public void testValidateBlueprint() throws Exception {

   }

   public void testValidateRoles() throws Exception {
   }

   public void testValidateCliConfigurations() throws Exception {

   }

   @Test
   public void testValidateRoleDependency() {
      ClusterBlueprint blueprint = new ClusterBlueprint();
      List<String> failedMsgList = new ArrayList<String>();
      assertEquals(false, validator.validateRoleDependency(failedMsgList, blueprint));

      NodeGroupInfo compute = new NodeGroupInfo();
      NodeGroupInfo data = new NodeGroupInfo();
      List<NodeGroupInfo> nodeGroupInfos = new ArrayList<NodeGroupInfo>();
      nodeGroupInfos.add(compute);
      nodeGroupInfos.add(data);
      blueprint.setNodeGroups(nodeGroupInfos);
      assertEquals(false, validator.validateRoleDependency(failedMsgList, blueprint));
      assertEquals(2, failedMsgList.size());
      failedMsgList.clear();
      blueprint.setExternalHDFS("hdfs://192.168.0.2:9000");
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      assertEquals(false, validator.validateRoleDependency(failedMsgList, blueprint));
      assertEquals(2, failedMsgList.size());
      assertEquals("Duplicate NameNode or DataNode role.", failedMsgList.get(0));
      assertEquals("Missing JobTracker or ResourceManager role.",
            failedMsgList.get(1));
      failedMsgList.clear();
      blueprint.setExternalHDFS("");
      nodeGroupInfos = new ArrayList<NodeGroupInfo>();
      nodeGroupInfos.add(compute);
      blueprint.setNodeGroups(nodeGroupInfos);
      assertEquals(false, validator.validateRoleDependency(failedMsgList, blueprint));
      assertEquals(1, failedMsgList.size());
      assertEquals("Missing role(s): hadoop_jobtracker for service: MAPRED.", failedMsgList.get(0));
      failedMsgList.clear();
      NodeGroupInfo master = new NodeGroupInfo();
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_JOBTRACKER_ROLE
            .toString()));
      nodeGroupInfos = new ArrayList<NodeGroupInfo>();
      nodeGroupInfos.add(master);
      nodeGroupInfos.add(compute);
      blueprint.setNodeGroups(nodeGroupInfos);
      assertEquals(false, validator.validateRoleDependency(failedMsgList, blueprint));
      assertEquals(1, failedMsgList.size());
      assertEquals("Some dependent services " + EnumSet.of(ServiceType.HDFS)
            + " " + ServiceType.MAPRED
            + " relies on cannot be found in the spec file.",
            failedMsgList.get(0));
   }

   @Test
   public void testValidateInfraSettingsBasedOnRole() {
      ClusterBlueprint blueprint = new ClusterBlueprint();
      blueprint.setHadoopStack(new HadoopStack());
      blueprint.getHadoopStack().setDistro(Constants.DEFAULT_VENDOR);

      Map<NetTrafficType, List<String>> networkConfig = new HashMap<NetTrafficType, List<String>>();

      List<String> mgtnets = new ArrayList<String>();
      mgtnets.add("nw1");
      networkConfig.put(NetTrafficType.MGT_NETWORK, mgtnets);

      List<String> hadpnets = new ArrayList<String>();
      hadpnets.add("nw2");
      networkConfig.put(NetTrafficType.HDFS_NETWORK, hadpnets);

      NodeGroupInfo master = new NodeGroupInfo();
      master.setName("master");
      master.setInstanceNum(1);
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_NAMENODE_ROLE.toString(),
            HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()));
      NodeGroupInfo worker = new NodeGroupInfo();
      worker.setName("worker");
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString(),
            HadoopRole.HADOOP_NODEMANAGER_ROLE.toString()));
      worker.setInstanceNum(0);
      NodeGroupInfo client = new NodeGroupInfo();
      client.setName("client");
      client.setInstanceNum(0);
      client.setRoles(Arrays.asList(HadoopRole.HADOOP_CLIENT_ROLE.toString(),
            HadoopRole.HIVE_SERVER_ROLE.toString(),
            HadoopRole.HIVE_ROLE.toString()));
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      List<NodeGroupInfo> groups = new ArrayList<NodeGroupInfo>();
      groups.add(master);
      groups.add(worker);
      groups.add(client);
      blueprint.setNodeGroups(groups);
      validator.validateGroupConfig(blueprint, failedMsgList, warningMsgList);
      assertEquals(1, failedMsgList.size());
      assertEquals("worker.instanceNum=0.", failedMsgList.get(0));
      assertEquals(0, warningMsgList.size());
   }

   @Test
   public void testIsComputeOnly() {
      List<String> roles = Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString());
      assertEquals(true, defaultSoftwareManager.isComputeOnlyRoles(roles));
      roles = Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.TEMPFS_CLIENT_ROLE.toString());
      assertEquals(true, defaultSoftwareManager.isComputeOnlyRoles(roles));
      roles = Arrays.asList(HadoopRole.MAPR_TASKTRACKER_ROLE.toString());
      assertEquals(true, defaultSoftwareManager.isComputeOnlyRoles(roles));
      roles = Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.HADOOP_DATANODE.toString());
      assertEquals(false, defaultSoftwareManager.isComputeOnlyRoles(roles));
   }

   @Test
   public void testContainsComputeOnlyNodeGroups() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate compute = new NodeGroupCreate();
      NodeGroupCreate data = new NodeGroupCreate();
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(true, cluster.containsComputeOnlyNodeGroups(defaultSoftwareManager));
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.TEMPFS_CLIENT_ROLE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { compute, data });
      assertEquals(true, cluster.containsComputeOnlyNodeGroups(defaultSoftwareManager));
      NodeGroupCreate worker = new NodeGroupCreate();
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.HADOOP_DATANODE.toString()));
      cluster.setNodeGroups(new NodeGroupCreate[] { worker });
      assertEquals(false, cluster.containsComputeOnlyNodeGroups(defaultSoftwareManager));
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testValidateSetManualElasticity() {
      ClusterRead cluster = new ClusterRead();
      cluster.setDistroVendor(Constants.MAPR_VENDOR);
      NodeGroupRead compute = new NodeGroupRead();
      compute.setName("compute");
      compute.setRoles(Arrays.asList("mapr_tasktracker"));
      compute.setComputeOnly(defaultSoftwareManager.isComputeOnlyRoles(compute.getRoles()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
      compute.setRoles(Arrays.asList(
            "mapr_tasktracker",
      "mapr_nfs"));
      compute.setComputeOnly(defaultSoftwareManager.isComputeOnlyRoles(compute.getRoles()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(false,
            cluster.validateSetManualElasticity(Arrays.asList("compute")));
      cluster.setDistroVendor(Constants.APACHE_VENDOR);
      compute.setRoles(Arrays.asList("hadoop_tasktracker"));
      compute.setComputeOnly(defaultSoftwareManager.isComputeOnlyRoles(compute.getRoles()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
      compute.setRoles(Arrays.asList("hadoop_tasktracker",
      "tempfs_client"));
      compute.setComputeOnly(defaultSoftwareManager.isComputeOnlyRoles(compute.getRoles()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
   }

   @Test
   public void testValidateSetParamParameters() {
      List<String> roles1 = new LinkedList<String>();
      roles1.add("hadoop_tasktracker");
      NodeGroupRead ngr1 = new NodeGroupRead();
      ngr1.setInstanceNum(6);
      ngr1.setRoles(roles1);
      ngr1.setComputeOnly(defaultSoftwareManager.isComputeOnlyRoles(ngr1.getRoles()));
      List<NodeGroupRead> nodeGroupRead = new LinkedList<NodeGroupRead>();
      nodeGroupRead.add(ngr1);
      ClusterRead cluster = new ClusterRead();
      cluster.setNodeGroups(nodeGroupRead);
      cluster.setVhmMinNum(-1);
      cluster.setVhmMaxNum(-1);

      try {
         cluster.validateSetParamParameters(null, -2, null);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: minComputeNodeNum=-2. Value must be less than or equal to the number of compute-only nodes (6) and less than or equal to maxComputeNodeNum.", e.getMessage());
      }

      try {
         cluster.validateSetParamParameters(null, null, -2);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: maxComputeNodeNum=-2. Value must be less than or equal to the number of compute-only nodes (6) and greater than or equal to minComputeNodeNum.", e.getMessage());
      }

      try {
         cluster.validateSetParamParameters(9, null, null);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: targetComputeNodeNum=9. Value must be less than or equal to the number of compute-only nodes (6).", e.getMessage());
      }

      try {
         cluster.validateSetParamParameters(null, 6, 1);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: minComputeNodeNum=6. Value must be less than or equal to the number of compute-only nodes (6) and less than or equal to maxComputeNodeNum (1).", e.getMessage());
      }

      cluster.setVhmMinNum(6);
      try {
         cluster.validateSetParamParameters(null, null, 5);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: maxComputeNodeNum=5. Value must be less than or equal to the number of compute-only nodes (6) and greater than or equal to minComputeNodeNum (6).", e.getMessage());
      }

      cluster.setVhmMaxNum(1);
      try {
         cluster.validateSetParamParameters(null, 6, null);
         fail();
      } catch (BddException e) {
         assertEquals("Invalid value: minComputeNodeNum=6. Value must be less than or equal to the number of compute-only nodes (6) and less than or equal to maxComputeNodeNum (1).", e.getMessage());
      }

      //test will fail if Exception is thrown out
      cluster.setVhmMinNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, null, -1));
      assertEquals(true, cluster.validateSetParamParameters(null, null, 0));
      assertEquals(true, cluster.validateSetParamParameters(null, null, 1));

      cluster.setVhmMinNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, null, -1));
      assertEquals(true, cluster.validateSetParamParameters(null, null, 0));
      assertEquals(true, cluster.validateSetParamParameters(null, null, 1));

      cluster.setVhmMinNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, -1, null));
      cluster.setVhmMinNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, 0, null));
      cluster.setVhmMinNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, 1, null));

      cluster.setVhmMinNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, -1, null));
      cluster.setVhmMinNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, 0, null));
      cluster.setVhmMinNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, 1, null));


      cluster.setVhmMaxNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, null, -1));
      cluster.setVhmMaxNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, null, 0));
      cluster.setVhmMaxNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, null, 1));

      cluster.setVhmMaxNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, null, -1));
      cluster.setVhmMaxNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, null, 0));
      cluster.setVhmMaxNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, null, 1));

      cluster.setVhmMaxNum(0);
      assertEquals(true, cluster.validateSetParamParameters(null, -1, null));
      assertEquals(true, cluster.validateSetParamParameters(null, 0, null));
      //assertEquals(true, cluster.validateSetParamParameters(null, 1, null));

      cluster.setVhmMaxNum(-1);
      assertEquals(true, cluster.validateSetParamParameters(null, -1, null));
      assertEquals(true, cluster.validateSetParamParameters(null, 0, null));
      assertEquals(true, cluster.validateSetParamParameters(null, 1, null));

      cluster.validateSetParamParameters(null, null, null);

      cluster.validateSetParamParameters(2, null, null);
      cluster.validateSetParamParameters(null, 1, 5);
   }

   @Test
   public void testSortingNodeGroups() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate client = new NodeGroupCreate();
      client.setRoles(Arrays.asList("hadoop_client"));
      NodeGroupCreate worker = new NodeGroupCreate();
      worker.setRoles(Arrays.asList("hadoop_tasktracker",
      "hadoop_datanode"));
      NodeGroupCreate master = new NodeGroupCreate();
      master.setRoles(Arrays.asList("hadoop_namenode",
      "hadoop_jobtracker"));
      cluster.setNodeGroups(new NodeGroupCreate[] { client, worker, master });
      assertEquals(3, cluster.getNodeGroups().length);
      ClusterBlueprint blueprint = cluster.toBlueprint();
      defaultSoftwareManager.updateInfrastructure(blueprint);
      assertEquals(master.getName(), blueprint.getNodeGroups().get(0).getName());
      assertEquals(worker.getName(), blueprint.getNodeGroups().get(1).getName());
      assertEquals(client.getName(), blueprint.getNodeGroups().get(2).getName());
   }

   public void testValidateHDFSUrl() throws Exception {

   }

   public void testValidateScaling() throws Exception {

   }

   @Test
   public void testHasComputeMasterGroup() {
      ClusterBlueprint blueprint = new ClusterBlueprint();
      HadoopStack hadoopStack = new HadoopStack();
      hadoopStack.setVendor(Constants.DEFAULT_VENDOR);
      blueprint.setHadoopStack(hadoopStack);
      NodeGroupInfo compute = new NodeGroupInfo();
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString()));
      List<NodeGroupInfo> nodeGroupInfos = new ArrayList<NodeGroupInfo>();
      nodeGroupInfos.add(compute);
      blueprint.setNodeGroups(nodeGroupInfos);
      assertFalse(defaultSoftwareManager.hasComputeMasterGroup(blueprint));
      NodeGroupInfo master = new NodeGroupInfo();
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()));
      nodeGroupInfos.add(master);
      blueprint.setNodeGroups(nodeGroupInfos);
      assertTrue(defaultSoftwareManager.hasComputeMasterGroup(blueprint));
   }

   @Test
   public void testValidateGroupConfig() throws Exception {
      ClusterCreate cluster =
         TestFileUtil
         .getSimpleClusterSpec(TestFileUtil.HDFS_HA_CLUSTER_FILE);
      cluster.setDistro("bigtop");
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      validator.validateGroupConfig(cluster.toBlueprint(), failedMsgList, warningMsgList);
      assertTrue("Should get empty fail message.", failedMsgList.isEmpty());
      assertTrue("Should get empty warning message.", warningMsgList.isEmpty());
   }
}