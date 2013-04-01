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
package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrict;
import mockit.Verifications;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.spectypes.VcCluster;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */

public class ResourcePoolServiceTest extends BaseResourceTest {

   private static final Logger logger = Logger
         .getLogger(ResourcePoolServiceTest.class);

   private ResourcePoolService rpSvc;

   @NonStrict
   private IResourcePoolDAO rpDao;
   @Mocked
   private IResourceService resService;
   @Mocked
   private IClusterDAO clusterDao;


   @BeforeClass
   public void beforeClass() {
      rpSvc = new ResourcePoolService();
   }

   @Test(groups = { "res-mgmt" })
   public void testGetAllResourcePool() {
      logger.debug("test getAllResorucePool");
      new Expectations() {
         {
            List<VcResourcePoolEntity> rpEntities = new ArrayList<VcResourcePoolEntity>();
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            rpEntities.add(rpEntity);
            rpDao.findAllOrderByClusterName();
            result = rpEntities;
         }
      };
      rpSvc.setRpDao(rpDao);
      List<VcCluster> clusters = rpSvc.getAllVcResourcePool();
      Assert.assertNotNull(clusters);
      Assert.assertEquals(clusters.size(), 1);
   }

   @Test(groups = { "res-mgmt" })
   public void testAddResourcePool() {
      new Expectations() {
         {
            rpDao.isRPAdded(anyString, anyString);
            result = false;
            resService.isRPExistInVc(anyString, anyString);
            result = true;
         }
      };
      rpSvc.setRpDao(rpDao);
      rpSvc.setResService(resService);
      rpSvc.addResourcePool("testRP", "cluster-ws", "jarred");
      new Verifications(){{
         rpDao.addResourcePoolEntity("testRP", "cluster-ws", "jarred");
      }};
   }

   @Test(groups = { "res-mgmt" }, expectedExceptions = VcProviderException.class)
   public void testAddDuplicateResourcePool() {
      new Expectations() {
         {
            rpDao.isRPAdded(anyString, anyString);
            result = true;
         }
      };
      rpSvc.setRpDao(rpDao);
      rpSvc.setResService(resService);

      rpSvc.addResourcePool("testRP", "cluster-ws", "jarred");
   }

   @Test(groups = { "res-mgmt" }, expectedExceptions = VcProviderException.class)
   public void testAddNonExistResourcePool() {
      new Expectations() {
         {
            rpDao.isRPAdded(anyString, anyString);
            result = false;
            resService.isRPExistInVc(anyString, anyString);
            result = false;
         }
      };
      rpSvc.setRpDao(rpDao);
      rpSvc.setResService(resService);

      rpSvc.addResourcePool("testRP", "cluster-ws", "jarred");
   }

   @Test(groups = { "res-mgmt" })
   public void testGetVcResourcePoolByName() {
      new Expectations() {
         {
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            returns(rpEntity);
         }
      };
      rpSvc.setRpDao(rpDao);
      List<VcCluster> clusters = rpSvc.getVcResourcePoolByName("defaultRP");
      Assert.assertNotNull(clusters);
      Assert.assertEquals(clusters.size(), 1);
   }

   @Test(groups = { "res-mgmt" })
   public void testGetVcResourcePoolByNameList() {
      new Expectations() {
         {
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            VcResourcePoolEntity rpEntity2 = new VcResourcePoolEntity();
            rpEntity2.setName("defaultRP2");
            rpEntity2.setVcCluster("cluster-ws2");
            rpEntity2.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            returns(rpEntity,rpEntity2);
         }
      };
      rpSvc.setRpDao(rpDao);
      List<VcCluster> clusters = rpSvc.getVcResourcePoolByNameList(new String[]{"defaultRP","defaultRP2"});
      Assert.assertNotNull(clusters);
      Assert.assertEquals(clusters.size(), 2);
   }

   @Test(groups = { "res-mgmt" })
   public void testGetAllRPNames() {
      new Expectations() {
         {
            List<VcResourcePoolEntity> rpEntities = new ArrayList<VcResourcePoolEntity>();
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            rpEntities.add(rpEntity);
            rpDao.findAllOrderByClusterName();
            result = rpEntities;
         }
      };
      rpSvc.setRpDao(rpDao);
      Set<String> rpNames = rpSvc.getAllRPNames();
      Assert.assertNotNull(rpNames);
      Assert.assertEquals(rpNames.size(), 1);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC", "dependsOnDB"})
   public void testgetAllResourcePoolForRest() {
      super.init();
      IResourcePoolService rpSvc = ctx.getBean(IResourcePoolService.class);
      List<ResourcePoolRead> rps = rpSvc.getAllResourcePoolForRest();
      Assert.assertNotNull(rps);
   }

   @Test(groups = { "res-mgmt" })
   public void testDeleteResourcePool() {
      new Expectations() {
         {
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            returns(rpEntity);
            clusterDao.findClustersByUsedResourcePool(anyString);
            result = new ArrayList<String>();
         }
      };
      rpSvc.setRpDao(rpDao);
      rpSvc.setClusterDao(clusterDao);
      rpSvc.deleteResourcePool("testRP");
   }

   @Test(groups = { "res-mgmt" }, expectedExceptions=VcProviderException.class)
   public void testDeleteResourcePoolWithReference() {
      new Expectations() {
         {
            VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
            rpEntity.setName("defaultRP");
            rpEntity.setVcCluster("cluster-ws");
            rpEntity.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            returns(rpEntity);
            List<String> clusters = new ArrayList<String>();
            clusters.add("cluster1");
            clusterDao.findClustersByUsedResourcePool(anyString);
            result = clusters;
         }
      };
      rpSvc.setRpDao(rpDao);
      rpSvc.setClusterDao(clusterDao);
      rpSvc.deleteResourcePool("testRP");
   }
}
