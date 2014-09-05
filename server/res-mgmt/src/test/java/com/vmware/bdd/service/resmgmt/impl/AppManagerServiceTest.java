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

package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.springframework.util.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.dal.IAppManagerDAO;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.utils.Constants;

/**
 * @author XiaoNing(Blake) Zhang
 * 2014-09-04
 */
public class AppManagerServiceTest {
   @Mocked
   private IAppManagerDAO appManagerDAO;

   @Mocked
   private IClusterDAO clusterDAO;

   private AppManagerService appMgrSvc;
   private List<AppManagerEntity> appMgrList;
   private final AppManagerEntity appMgrEntity = new AppManagerEntity();


   @BeforeClass
   public void beforeClass() {
      appMgrSvc = new AppManagerService();

      appMgrEntity.setName("testAppMgr");
      appMgrEntity.setType(Constants.CLOUDERA_MANAGER_PLUGIN_TYPE);
      appMgrEntity.setUrl("http://10.141.73.106:7180");

      appMgrList = new ArrayList<AppManagerEntity>();
      appMgrList.add(appMgrEntity);
   }

   @Test(groups = { "res-mgmt" })
   public void testAddAppManager() {
      appMgrSvc.setAppManagerDAO(appManagerDAO);

      AppManagerAdd appMgr = new AppManagerAdd();
      appMgr.setType(Constants.AMBARI_PLUGIN_TYPE);
      appMgr.setUrl("http://10.141.73.200:7180");
      appMgrSvc.addAppManager(appMgr);

      new Verifications() {
         {
            AppManagerEntity entity = new AppManagerEntity();
            appManagerDAO.insert(withAny(entity));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void testDeleteAppManager() {
      new NonStrictExpectations() {
         {
            appManagerDAO.findByName(anyString);
            result = appMgrList;
            List<String> clusters = new ArrayList<String>();
            clusterDAO.findClustersByAppManager(anyString);
            result = clusters;
         }
      };

      appMgrSvc.setAppManagerDAO(appManagerDAO);
      appMgrSvc.setClusterDao(clusterDAO);
      appMgrSvc.deleteAppManager("testAppMgr");

      new Verifications() {
         {
            appManagerDAO.delete(withAny(appMgrEntity));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void testModifyAppManager() {
      new NonStrictExpectations() {
         {
            appManagerDAO.findByName(anyString);
            result = appMgrEntity;
         }
      };

      appMgrSvc.setAppManagerDAO(appManagerDAO);

      AppManagerAdd appMgr = new AppManagerAdd();
      appMgr.setType(Constants.AMBARI_PLUGIN_TYPE);
      appMgr.setUrl("http://10.141.73.200:7180");
      appMgrSvc.modifyAppManager(appMgr);

      new Verifications() {
         {
            appManagerDAO.update(withAny(appMgrEntity));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void testGetAllAppManagerRead() {
      new NonStrictExpectations() {
         {
            appManagerDAO.findByName(anyString);
            result = appMgrEntity;
         }
      };

      appMgrSvc.setAppManagerDAO(appManagerDAO);
      AppManagerRead appMgrRead = appMgrSvc.getAppManagerRead("testAppMgr");

      Assert.notNull(appMgrRead);
   }

   @Test(groups = { "res-mgmt" })
   public void testGetAllAppManagerReads() {
      new NonStrictExpectations() {
         {
            appManagerDAO.findAllSortByName();
            result = appMgrList;
         }
      };

      appMgrSvc.setAppManagerDAO(appManagerDAO);
      List<AppManagerRead> appMgrReads = appMgrSvc.getAllAppManagerReads();

      Assert.notNull(appMgrReads);
   }

}
