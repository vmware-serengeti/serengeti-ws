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
package com.vmware.bdd.dal;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import junit.framework.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 6/28/13
 * Time: 9:34 AM
 */
public class TestNodeDAO {

   private static final Logger logger = Logger.getLogger(TestNodeDAO.class);
   private static final String NODE1_NAME = "node1";
   private static final String NODE2_NAME = "node2";
   private static final String NODE1_MOID = "vm-001";
   private static final String NODE2_MOID = "vm-002";
   private static final String NODEGROUP_NAME = "ng1";

   private ApplicationContext ctx;
   private INodeDAO nodeDAO;
   private INodeGroupDAO ngDAO;

   private NodeEntity node1;
   private NodeEntity node2;
   private NodeGroupEntity ng;

   @BeforeClass
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("/META-INF/spring/*-context.xml");
      nodeDAO = ctx.getBean(INodeDAO.class);
      ngDAO = ctx.getBean(INodeGroupDAO.class);

      ng = new NodeGroupEntity();
      ng.setName(NODEGROUP_NAME);
      ng.setDefineInstanceNum(5);

      node1 = new NodeEntity();
      node1.setVmName(NODE1_NAME);
      node1.setMoId(NODE1_MOID);
      node1.setNodeGroup(ng);

      node2 = new NodeEntity();
      node2.setVmName(NODE2_NAME);
      node2.setMoId(NODE2_MOID);
      node2.setNodeGroup(ng);

      ngDAO.insert(ng);
      nodeDAO.insert(node1);
      nodeDAO.insert(node2);
   }

   @AfterClass
   public void clean() {
      nodeDAO.delete(node1);
      nodeDAO.delete(node2);
      ngDAO.delete(ng);
   }

   @Test
   public void testFindByNodeGroups() {
      Collection<NodeGroupEntity> ngs = new ArrayList<NodeGroupEntity>();
      ngs.add(ng);
      List<NodeEntity> nodes = nodeDAO.findByNodeGroups(ngs);
      Assert.assertTrue(nodes.size() == 2 && nodes.contains(node1) && nodes.contains(node2));
   }

   @Test
   public void testFindByName() {
      NodeEntity node = nodeDAO.findByName(ng, NODE1_NAME);
      Assert.assertTrue(node != null && node.getVmName().equals(NODE1_NAME));

      node = nodeDAO.findByName(NODE2_NAME);
      Assert.assertTrue(node != null && node.getVmName().equals(NODE2_NAME));
   }

   @Test
   public void testFindByMoid() {
      NodeEntity node = nodeDAO.findByMobId(NODE1_MOID);
      Assert.assertTrue(node != null && node.getVmName().equals(NODE1_NAME));
   }
}






