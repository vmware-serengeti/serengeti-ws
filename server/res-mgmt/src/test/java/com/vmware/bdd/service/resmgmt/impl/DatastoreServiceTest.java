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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.entity.VcDatastoreEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.IResourceService;

public class DatastoreServiceTest extends BaseResourceTest {

   @Mocked
   private IDatastoreDAO dsDao;
   @Mocked
   private IResourceService resService;
   @Mocked
   private IClusterDAO clusterDao;

   private DatastoreService datastoreSvc;

   private List<VcDatastoreEntity> dss;
   private final VcDatastoreEntity ds = new VcDatastoreEntity();

   @BeforeClass
   public void beforeClass() {
      datastoreSvc = new DatastoreService();
      dss = new ArrayList<VcDatastoreEntity>();
      ds.setName("testDS");
      ds.setType(DatastoreType.SHARED);
      ds.setVcDatastore("datastore");
      dss.add(ds);
   }


   @Test(groups = { "res-mgmt" })
   public void addDataStores() {
      new NonStrictExpectations() {
         {
            dsDao.nameExisted(anyString);
            result = false;
            resService.isDatastoreExistInVC(anyString);
            result = true;
         }
      };
      datastoreSvc.setDsDao(dsDao);
      datastoreSvc.setResService(resService);
      List<String> dsSpec = new ArrayList<String>();
      dsSpec.add("datastore");
      datastoreSvc.addDataStores("testDS", DatastoreType.SHARED, dsSpec);

      new Verifications() {
         {
            VcDatastoreEntity entity = new VcDatastoreEntity();
            dsDao.insert(withAny(entity));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void deleteDatastore() {
      new NonStrictExpectations() {
         {
            dsDao.findByName(anyString);
            result = dss;
            Set<String> names = new HashSet<String>();
            names.add("datastore1");
            List<String> clusters = new ArrayList<String>();
            clusterDao.findClustersByUsedDatastores(names);
            result = clusters;
         }
      };
      datastoreSvc.setDsDao(dsDao);
      datastoreSvc.setClusterDao(clusterDao);
      datastoreSvc.deleteDatastore("testDS");

      new Verifications() {
         {
            dsDao.delete(withAny(ds));
         }
      };
   }

   @Test(groups = { "res-mgmt" }, expectedExceptions = VcProviderException.class)
   public void deleteDatastoreWithReference() {
      new NonStrictExpectations() {
         {
            dsDao.findByName(anyString);
            result = dss;
            Set<String> names = new HashSet<String>();
            names.add("datastore");
            List<String> clusters = new ArrayList<String>();
            clusters.add("cluster1");
            clusterDao.findClustersByUsedDatastores(names);
            returns(clusters);
         }
      };
      datastoreSvc.setDsDao(dsDao);
      datastoreSvc.setClusterDao(clusterDao);
      datastoreSvc.deleteDatastore("testDS");
   }

   @Test(groups = { "res-mgmt" })
   public void getAllDataStoreName() {
      new Expectations() {
         {
            dsDao.findAllSortByName();
            result = dss;
         }
      };
      datastoreSvc.setDsDao(dsDao);
      Set<String> names = datastoreSvc.getAllDataStoreName();
      Assert.assertNotNull(names);
      Assert.assertEquals(names.size(), 1);
   }

   @Test(groups = { "res-mgmt" })
   public void getAllDatastoreReads() {
      new Expectations() {
         {
            dsDao.findByName(anyString);
            result = dss;
         }
      };
      datastoreSvc.setDsDao(dsDao);
      DatastoreRead ds = datastoreSvc.getDatastoreRead("testDS");
      Assert.assertNotNull(ds);
   }

}