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

import java.util.Arrays;
import java.util.List;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 9/4/14.
 */
public class TestSWMgrCollector_GetAppMgrReadAndDelete extends TestSWMgrCollectorBase{

   @Test
   public void testGetAppMgrRead_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(defaultAppManagerEntity));

      softwareManagerCollector.loadSoftwareManagers();

      Mockito.when(appManagerService.getAppManagerRead(Matchers.anyString())).thenReturn(defaultAppManagerRead);

      AppManagerRead appManagerRead = softwareManagerCollector.getAppManagerRead(defaultAppManagerAdd.getName());

      Assert.assertEquals(defaultAppManagerRead.getName(), appManagerRead.getName());
      Assert.assertEquals(defaultAppManagerRead.getType(), appManagerRead.getType());
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Cannot find app manager Default.")
   public void testGetAppMgrRead_NotFound() {
      Mockito.when(appManagerService.getAppManagerRead(Matchers.anyString())).thenReturn(null);

      AppManagerRead appManagerRead = softwareManagerCollector.getAppManagerRead(defaultAppManagerAdd.getName());
   }

   @Test
   public void testListAppMgrReads_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(defaultAppManagerEntity));

      softwareManagerCollector.loadSoftwareManagers();

      Mockito.when(appManagerService.getAllAppManagerReads()).thenReturn(Arrays.asList(defaultAppManagerRead));

      List<AppManagerRead> appManagerRead = softwareManagerCollector.getAllAppManagerReads();

      Assert.assertEquals(1, appManagerRead.size());

      Assert.assertEquals(defaultAppManagerRead.getName(), appManagerRead.get(0).getName());
      Assert.assertEquals(defaultAppManagerRead.getType(), appManagerRead.get(0).getType());
   }

   @Test
   public void testGetAppManagerTypes() {
      Configuration.setString("appmanager.types", "ClouderaManager, Ambari");

      List<String> types = softwareManagerCollector.getAllAppManagerTypes();

      Assert.assertEquals(new String[]{"ClouderaManager", "Ambari"}, types.toArray(new String[]{}));
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Cannot find app manager fooAppMgr.")
   public void testDeleteSWMgr_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(appManagerEntityFoo));

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + appManagerEntityFoo.getType(), "com.vmware.bdd.manager.mocks.FooSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");

      softwareManagerCollector.loadSoftwareManagers();

      SoftwareManager appManagerRead = softwareManagerCollector.getSoftwareManager(appManagerEntityFoo.getName());

      Assert.assertEquals(appManagerAddFoo.getName(), appManagerRead.getName());
      Assert.assertEquals(appManagerAddFoo.getType(), appManagerRead.getType());


      softwareManagerCollector.deleteSoftwareManager(appManagerAddFoo.getName());

      softwareManagerCollector.getSoftwareManager(appManagerEntityFoo.getName());
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Cannot delete default application manager.")
   public void testDeleteSWMgr_CantDelDefault() {
      softwareManagerCollector.deleteSoftwareManager(Constants.IRONFAN);
   }
}

