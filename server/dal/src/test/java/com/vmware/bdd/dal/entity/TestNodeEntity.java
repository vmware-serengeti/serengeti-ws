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

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.dal.INodeDAO;
import com.vmware.bdd.entity.NodeEntity;
import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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


}
