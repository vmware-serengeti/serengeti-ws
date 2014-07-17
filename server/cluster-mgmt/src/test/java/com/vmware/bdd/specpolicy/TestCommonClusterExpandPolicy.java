/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved
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
package com.vmware.bdd.specpolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.InstanceType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.plugin.ironfan.impl.DefaultSoftwareManagerImpl;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.IronfanStack;

public class TestCommonClusterExpandPolicy {

   private static SoftwareManager softwareManager;

   @BeforeClass(groups = { "TestCommonClusterExpandPolicy" })
   public static void setup() {
      softwareManager = new DefaultSoftwareManagerImpl();
   }

   @Test(groups = { "TestCommonClusterExpandPolicy" })
   public void testExpandGroupInstanceType() {
      List<String> roles = new ArrayList<String>();
      roles.add(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString());
      NodeGroupCreate nodeGroupCreate = new NodeGroupCreate();
      nodeGroupCreate.setName("resourceManager");
      nodeGroupCreate.setRoles(roles);
      nodeGroupCreate.setStorage(new StorageRead());
      nodeGroupCreate.setCpuNum(Integer.valueOf(2));
      nodeGroupCreate.setMemCapacityMB(Integer.valueOf(3748));
      NodeGroupEntity nodeGroupEntity = new NodeGroupEntity();
      nodeGroupEntity.setName(nodeGroupCreate.getName());
      nodeGroupEntity.setCpuNum(nodeGroupCreate.getCpuNum());
      nodeGroupEntity.setMemorySize(nodeGroupCreate.getMemCapacityMB());
      CommonClusterExpandPolicy.expandGroupInstanceType(nodeGroupEntity,
            nodeGroupCreate, null, null, softwareManager);
      assertEquals(nodeGroupEntity.getNodeType(), InstanceType.MEDIUM);
      assertEquals(nodeGroupEntity.getStorageSize(), 50);
   }

   @Test(groups = { "TestCommonClusterExpandPolicy" })
   public void testExpandDistro() {
      final String hadoopUrl = "apache/1.2.1/hadoop-1.2.1.tar.gz";
      final String zookeeperUrl = "apache/1.2.1/zookeeper-3.4.5.tar.gz";
      final String bigTopRepoUrl = "https://192.168.0.1/yum/bigtop.repo";
      ClusterCreate clusterConfig = new ClusterCreate();
      IronfanStack stack = new IronfanStack();
      stack.setPackagesExistStatus("TARBALL");
      Map<String, String> hadoopDistroMap = new HashMap<String, String>();
      hadoopDistroMap.put("HadoopUrl", hadoopUrl);
      hadoopDistroMap.put("ZookeeperUrl", zookeeperUrl);
      stack.setHadoopDistroMap(hadoopDistroMap);
      CommonClusterExpandPolicy.expandDistro(clusterConfig, stack);
      assertEquals(clusterConfig.getDistroMap().getHadoopUrl(), hadoopUrl);
      assertEquals(clusterConfig.getDistroMap().getZookeeperUrl(), zookeeperUrl);
      stack.setPackagesExistStatus("REPO");
      List<String> repos = new ArrayList<String>();
      repos.add(bigTopRepoUrl);
      stack.setPackageRepos(repos);
      CommonClusterExpandPolicy.expandDistro(clusterConfig, stack);
      assertEquals(clusterConfig.getPackageRepos().get(0), bigTopRepoUrl);
   }
}
