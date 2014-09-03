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
import java.util.Arrays;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 8/28/14.
 */
public class TestSWMgrCollector_LoadAppManager extends TestSWMgrCollectorBase{

   @Test
   public void testLoadAppManagers_Default() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(defaultAppManagerEntity));

      softwareManagerCollector.loadSoftwareManagers();

      assertSoftwareManagers(softwareManagerCollector.getSoftwareManager(Constants.IRONFAN), defaultAppManagerAdd);
   }

   @Test
   public void testLoadAppManagers_Upgrade() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      softwareManagerCollector.loadSoftwareManagers();

      assertSoftwareManagers(softwareManagerCollector.getSoftwareManager(Constants.IRONFAN), defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Cannot find app manager fooAppMgr.")
   public void testLoadAppManagers_Exceptional() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(appManagerEntityFoo, defaultAppManagerEntity));

      softwareManagerCollector.loadSoftwareManagers();

      assertSoftwareManagers(softwareManagerCollector.getSoftwareManager(Constants.IRONFAN), defaultAppManagerAdd);
      softwareManagerCollector.getSoftwareManager(appManagerAddFoo.getName());
   }

   @Test
   public void testLoadAppManager_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + defaultAppManagerAdd.getType(), "com.vmware.bdd.manager.mocks.DefaultSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.loadSoftwareManager(defaultAppManagerAdd);

      assertSoftwareManagers(softwareManagerCollector.getSoftwareManager(Constants.IRONFAN), defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SWMgrCollectorInternalException.class,
   expectedExceptionsMessageRegExp = "AppManager factory class for Default is not defined in serengeti.properties.")
   public void testLoadAppManager_FactoryNotDefined() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + Constants.IRONFAN, "");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.loadSoftwareManager(defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Application manager Default status is unhealthy. Please check application manager console for more details.")
   public void testLoadAppManager_Unhealthy() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + Constants.IRONFAN, "com.vmware.bdd.manager.mocks.UnhealthySoftwareManagerFactory");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.loadSoftwareManager(defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SWMgrCollectorInternalException.class,
         expectedExceptionsMessageRegExp = "Failed to instantiate AppManager Factory: com.vmware.foo.")
   public void testLoadAppManager_CantInstantiateFactory() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + defaultAppManagerAdd.getType(), "com.vmware.foo");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.loadSoftwareManager(defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Cannot connect to application manager Default with error RuntimeException: , check the connection information.")
   public void testLoadAppManager_CantInstantiateAppMgr() {
      Mockito.when(appManagerService.findAll()).thenReturn(new ArrayList<AppManagerEntity>());

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + defaultAppManagerAdd.getType(), "com.vmware.bdd.manager.mocks.ExceptionalSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");


      softwareManagerCollector.loadSoftwareManager(defaultAppManagerAdd);

   }

   public static void assertSoftwareManagers(SoftwareManager softwareManager1, AppManagerAdd spec) {
      Assert.assertEquals(softwareManager1.getName(), spec.getName());
      Assert.assertEquals(softwareManager1.getType(), spec.getType());
   }

}
