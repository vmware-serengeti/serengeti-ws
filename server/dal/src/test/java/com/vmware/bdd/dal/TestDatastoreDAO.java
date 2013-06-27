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

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Datastore;
import com.vmware.bdd.entity.VcDatastoreEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/27/13
 * Time: 5:56 PM
 */

public class TestDatastoreDAO {
   private static final Logger logger = Logger.getLogger(TestDatastoreDAO.class);

   private ApplicationContext ctx;
   private IDatastoreDAO datastoreDAO;

   @BeforeClass
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("/META-INF/spring/*-context.xml");
      datastoreDAO = ctx.getBean(IDatastoreDAO.class);

      VcDatastoreEntity dsEntity1 = new VcDatastoreEntity();
      dsEntity1.setName("ds1");
      dsEntity1.setVcDatastore("test-ds1");
      dsEntity1.setType(Datastore.DatastoreType.LOCAL);

      VcDatastoreEntity dsEntity2 = new VcDatastoreEntity();
      dsEntity2.setName("ds2");
      dsEntity2.setVcDatastore("test-ds2");
      dsEntity2.setType(Datastore.DatastoreType.SHARED);

      VcDatastoreEntity dsEntity3 = new VcDatastoreEntity();
      dsEntity3.setName("ds3");
      dsEntity3.setVcDatastore("test-ds3");
      dsEntity3.setType(Datastore.DatastoreType.LOCAL);

      datastoreDAO.findAll();
      datastoreDAO.insert(dsEntity1);
      datastoreDAO.insert(dsEntity2);
      datastoreDAO.insert(dsEntity3);
   }

   @AfterClass
   public void clean() {
      List<VcDatastoreEntity> dsEntities =  datastoreDAO.findAll();
      for (VcDatastoreEntity ds : dsEntities) {
         if (ds.getName() == "ds1" || ds.getName() == "ds2" || ds.getName() == "ds3") {
            datastoreDAO.delete(ds);
         }
      }
   }

   @Test
   public void testFindAllSortByName() {
      List<VcDatastoreEntity> entities = datastoreDAO.findAllSortByName();
      Assert.assertTrue(entities.size() == 3);
      Assert.assertTrue(entities.get(0).getName().equals("ds1"));
      Assert.assertTrue(entities.get(1).getName().equals("ds2"));
      Assert.assertTrue(entities.get(2).getName().equals("ds3"));
   }

   @Test
   public void testFindByType() {
      List<VcDatastoreEntity> entities = datastoreDAO.findByType(Datastore.DatastoreType.LOCAL);
      Assert.assertTrue(entities.size() == 2);
   }

   @Test
   public void testFindByName() {
      List<VcDatastoreEntity> entities = datastoreDAO.findByName("ds2");
      logger.info("find: " + entities.get(0).getName());
      Assert.assertTrue(entities.size() == 1 && entities.get(0).getVcDatastore().equals("test-ds2"));
   }

   @Test
   public void testNameExisted() {
      Assert.assertTrue(datastoreDAO.nameExisted("ds3"));
      Assert.assertTrue(!datastoreDAO.nameExisted("ds4"));
   }
}
