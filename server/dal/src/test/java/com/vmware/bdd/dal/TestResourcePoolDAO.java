/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.entity.VcResourcePoolEntity;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 6/28/13
 * Time: 10:29 AM
 */
public class TestResourcePoolDAO {

   private final String RP1_NAME = "rp1";
   private final String RP2_NAME = "rp2";
   private final String CLUSTER_NAME = "cluster";
   private final String VC_RP1 = "test_rp1";
   private final String VC_RP2 = "test_rp2";
   private ApplicationContext ctx;
   private IResourcePoolDAO rpDAO;
   private VcResourcePoolEntity rp1;
   private VcResourcePoolEntity rp2;

   @BeforeClass
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("/META-INF/spring/*-context.xml");
      rpDAO = ctx.getBean(IResourcePoolDAO.class);

      rp1 = new VcResourcePoolEntity();
      rp1.setName(RP1_NAME);
      rp1.setVcCluster(CLUSTER_NAME);
      rp1.setVcResourcePool(VC_RP1);

      rp2 = new VcResourcePoolEntity();
      rp2.setName(RP2_NAME);
      rp2.setVcCluster(CLUSTER_NAME);
      rp2.setVcResourcePool(VC_RP2);

      rpDAO.insert(rp1);
      rpDAO.insert(rp2);
   }

   @Test
   public void testFindByName() {
      VcResourcePoolEntity rp = rpDAO.findByName(RP1_NAME);
      Assert.assertTrue(rp != null && rp.getName().equals(RP1_NAME));
   }

   @Test
   public void testIsRpAdded() {
      Assert.assertTrue(rpDAO.isRPAdded(CLUSTER_NAME, VC_RP1));
      Assert.assertTrue(!rpDAO.isRPAdded(CLUSTER_NAME, "test_rp3"));
   }

   @Test
   public void testGetNameByClusterAndRp() {
      Assert.assertTrue(rpDAO.getNameByClusterAndRp(CLUSTER_NAME, VC_RP2).equals(RP2_NAME));
      Assert.assertTrue(rpDAO.getNameByClusterAndRp(CLUSTER_NAME, "test_rp3") == null);
   }

   @Test
   public void testAddResourcePoolEntity() {
      rpDAO.addResourcePoolEntity("new_rp", "new_cluster", "new_vc_rp");
      VcResourcePoolEntity rp = rpDAO.findByName("new_rp");
      Assert.assertTrue(rp != null && rp.getVcCluster().equals("new_cluster"));
   }

}
