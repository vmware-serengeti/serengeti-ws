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
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import org.springframework.test.context.ContextConfiguration;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 6/27/13
 * Time: 5:29 PM
 */

@ContextConfiguration(locations = {"classpath:/META-INF/spring/*-context.xml"})
public class TestVcResourcePoolEntity {
   private static final Logger logger = Logger.getLogger(TestVcResourcePoolEntity.class);

   private static final String ENTITY_NAME = "RP1";
   private static final String CLUSTER_NAME = "Cluster1";
   private static final String VC_RP = "TestRP1";
   @Test
   public void testToRest() {
      VcResourcePoolEntity vcRpEntity = new VcResourcePoolEntity();
      vcRpEntity.setName(ENTITY_NAME);
      vcRpEntity.setVcCluster(CLUSTER_NAME);
      vcRpEntity.setVcResourcePool(VC_RP);

      Set<NodeEntity> nodes = new HashSet<NodeEntity>();
      NodeEntity node1 = new NodeEntity();
      node1.setVmName("node1");
      NodeEntity node2 = new NodeEntity();
      node2.setVmName("node2");
      NodeGroupEntity ng = new NodeGroupEntity();
      ng.setRoles(new Gson().toJson(new String[] { "hadoop_datanode" }));
      node1.setNodeGroup(ng);
      node2.setNodeGroup(ng);
      nodes.add(node1);
      nodes.add(node2);

      vcRpEntity.setHadoopNodes(nodes);

      ResourcePoolRead rpRead = vcRpEntity.toRest();
      Assert.assertTrue(rpRead != null);

      NodeRead[] nodeReads = rpRead.getNodes();
      Assert.assertTrue(nodeReads.length == 2);
      Assert.assertTrue(nodeReads[0].getName().equals("node1"));
      Assert.assertTrue(nodeReads[1].getName().equals("node2"));
   }
}
