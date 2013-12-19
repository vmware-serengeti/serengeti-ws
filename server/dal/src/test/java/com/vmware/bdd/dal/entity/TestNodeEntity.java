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
package com.vmware.bdd.dal.entity;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.dal.INodeDAO;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.spectypes.NicSpec;
import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sun.swing.BakedArrayList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ContextConfiguration(locations = {"classpath:/META-INF/spring/*-context.xml"})
public class TestNodeEntity {
   private static final String NODE_NAME = "hadoop-node";

   @Autowired
   private INodeDAO nodeDAO;

   @BeforeMethod
   public void setup() {
   }

   @AfterMethod
   public void tearDown() {
   }

   @Test
   public void testSetStatus() {
      NodeEntity node = new NodeEntity();
      node.setVmName(NODE_NAME);
      node.setPowerStatusChanged(false);

      NodeStatus formerStatus = NodeStatus.VM_READY;
      NodeStatus newStatus = NodeStatus.BOOTSTRAP_FAILED;
      node.setStatus(formerStatus, false);
      node.setStatus(newStatus);
      Assert.assertTrue(node.getStatus() == NodeStatus.BOOTSTRAP_FAILED);
      Assert.assertTrue(!node.isPowerStatusChanged());

      formerStatus = NodeStatus.NOT_EXIST;
      newStatus = NodeStatus.POWERED_OFF;
      node.setStatus(formerStatus, false);
      node.setStatus(newStatus);
      Assert.assertTrue(node.getStatus() == NodeStatus.POWERED_OFF);
      Assert.assertTrue(!node.isPowerStatusChanged());

      formerStatus = NodeStatus.POWERED_OFF;
      newStatus = NodeStatus.BOOTSTRAP_FAILED;
      node.setStatus(formerStatus, false);
      node.setStatus(newStatus);
      Assert.assertTrue(node.getStatus() == NodeStatus.BOOTSTRAP_FAILED);
      Assert.assertTrue(node.isPowerStatusChanged());
   }

   @Test void testToRead() {
      NodeEntity node = new NodeEntity();
      Set<NicEntity> nicEntitySet = new HashSet<NicEntity>();
      NicSpec.NetTrafficDefinition netDef1 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.MGT_NETWORK, 0);
      NicSpec.NetTrafficDefinition netDef2 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.MGT_NETWORK, 1);
      NicSpec.NetTrafficDefinition netDef3 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.MGT_NETWORK, 2);
      NicSpec.NetTrafficDefinition netDef4 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.HDFS_NETWORK, 0);
      NicSpec.NetTrafficDefinition netDef5 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.HDFS_NETWORK, 1);

      Set<NicSpec.NetTrafficDefinition> netDefs1 = new HashSet<NicSpec.NetTrafficDefinition>();
      netDefs1.add(netDef1);
      netDefs1.add(netDef4);

      Set<NicSpec.NetTrafficDefinition> netDefs2 = new HashSet<NicSpec.NetTrafficDefinition>();
      netDefs2.add(netDef2);
      netDefs2.add(netDef5);

      Set<NicSpec.NetTrafficDefinition> netDefs3 = new HashSet<NicSpec.NetTrafficDefinition>();
      netDefs3.add(netDef3);

      NicEntity nic1 = new NicEntity();
      NetworkEntity net1 = new NetworkEntity();
      net1.setPortGroup("pg1");
      net1.setName("net1");
      nic1.setIpv4Address("192.168.0.1");
      nic1.setNetTrafficDefs(netDefs1);
      nic1.setNetworkEntity(net1);

      NicEntity nic2 = new NicEntity();
      nic2.setIpv4Address("192.168.1.1");
      nic2.setNetTrafficDefs(netDefs2);
      NetworkEntity net2 = new NetworkEntity();
      net2.setPortGroup("pg2");
      net2.setName("net2");
      nic2.setNetworkEntity(net2);

      NicEntity nic3 = new NicEntity();
      nic3.setIpv4Address("192.168.2.1");
      nic3.setNetTrafficDefs(netDefs3);
      NetworkEntity net3 = new NetworkEntity();
      net3.setPortGroup("pg3");
      net3.setName("net3");
      nic3.setNetworkEntity(net3);

      nicEntitySet.add(nic1);
      nicEntitySet.add(nic2);
      nicEntitySet.add(nic3);

      node.setNics(nicEntitySet);
      node.setNodeGroup(new NodeGroupEntity());

      System.out.println((new Gson()).toJson(node.toNodeRead(false)));
   }
}
