/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.manager;

import java.util.ArrayList;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 8/28/14.
 */
public class TestSWMgrCollector_CreateAndGetByClusterName extends TestSWMgrCollectorBase{

   @Test
   public void testCreateAppManager_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + defaultAppManagerAdd.getType(), "com.vmware.bdd.manager.mocks.DefaultSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.createSoftwareManager(defaultAppManagerAdd);

      Mockito.when(appManagerService.findAppManagerByName(Matchers.anyString())).thenReturn(defaultAppManagerEntity);
      TestSWMgrCollector_LoadAppManager.assertSoftwareManagers(softwareManagerCollector.getSoftwareManager(Constants.IRONFAN), defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Invalid URL: URL should starts with http or https.")
   public void testCreateAppManager_InvalidURL() {
      AppManagerAdd appManagerAddDefault1 = new AppManagerAdd();
      appManagerAddDefault1.setName(Constants.IRONFAN);
      appManagerAddDefault1.setDescription(Constants.IRONFAN_DESCRIPTION);
      appManagerAddDefault1.setType(Constants.IRONFAN);
      appManagerAddDefault1.setUrl("ftp://address");
      appManagerAddDefault1.setUsername("");
      appManagerAddDefault1.setPassword("");
      appManagerAddDefault1.setSslCertificate("");

      softwareManagerCollector.createSoftwareManager(appManagerAddDefault1);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Name Default already exists.")
   public void testCreateAppManager_AlreadyExists() {
      Mockito.when(appManagerService.findAppManagerByName(Matchers.anyString())).thenReturn(defaultAppManagerEntity);

      softwareManagerCollector.createSoftwareManager(defaultAppManagerAdd);
   }

   @Test(expectedExceptions = BddException.class,
         expectedExceptionsMessageRegExp = "Internal error: Failed to write AppManager to META-DB.")
   public void testCreateAppManager_FailedWriteMetaDB() {
      Mockito.doThrow(new RuntimeException()).when(appManagerService).addAppManager(appManagerAddFoo);

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + appManagerAddFoo.getType(), "com.vmware.bdd.manager.mocks.FooSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");
      try {
         softwareManagerCollector.createSoftwareManager(appManagerAddFoo);
      } catch (Exception ex) {
         Assert.assertEquals(softwareManagerCollector.getCacheSize(), 0);
         throw ex;
      }
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Name fooAppMgr already exists.")
   public void testCreateAppManager_DuplicateInMetaDB() {
      Mockito.doThrow(SoftwareManagerCollectorException.DUPLICATE_NAME(appManagerAddFoo.getName())).when(appManagerService).addAppManager(appManagerAddFoo);

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + appManagerAddFoo.getType(), "com.vmware.bdd.manager.mocks.FooSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");

      try {
         softwareManagerCollector.createSoftwareManager(appManagerAddFoo);
      } catch (Exception ex) {
         Assert.assertEquals(softwareManagerCollector.getCacheSize(), 0);
         throw ex;
      }
   }

   @Test(expectedExceptions = SWMgrCollectorInternalException.class,
         expectedExceptionsMessageRegExp = "AppManager factory class for fooAppMgr is not defined in serengeti.properties.")
   public void testGetAppManager_FactoryUndefined() {
      Mockito.when(appManagerService.findAppManagerByName(Matchers.anyString())).thenReturn(appManagerEntityFoo);

      softwareManagerCollector.setPrivateKey("mock-key");

      softwareManagerCollector.getSoftwareManager("fooAppMgr");
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Cannot find app manager fooAppMgr.")
   public void testGetAppManager_AppMgrNotFound() {
      Mockito.when(appManagerService.findAppManagerByName(Matchers.anyString())).thenReturn(null);

      softwareManagerCollector.getSoftwareManager("fooAppMgr");
   }

   @Test
   public void testGetAppManagerByClusterName_OK() {
      testCreateAppManager_Success();

      ClusterEntity fooClusterEntity = new ClusterEntity("fooCluster");
      fooClusterEntity.setAppManager(defaultAppManagerAdd.getName());

      Mockito.when(clusterEntityManager.findByName(Matchers.anyString())).thenReturn(fooClusterEntity);

      SoftwareManager defaultSWMgr = softwareManagerCollector.getSoftwareManagerByClusterName("fooCluster");

      Assert.assertEquals(Constants.IRONFAN, defaultSWMgr.getName());
      Assert.assertEquals(Constants.IRONFAN, defaultSWMgr.getType());
   }

   @Test
   public void testGetAppManagerByClusterName_NotFound() {
      Mockito.when(clusterEntityManager.findByName(Matchers.anyString())).thenReturn(null);

      Assert.assertNull(softwareManagerCollector.getSoftwareManagerByClusterName("fooCluster"));
   }

}
