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

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;

/**
 * Created By xiaoliangl on 9/4/14.
 */
public class TestSWMgrCollector_Modify extends TestSWMgrCollectorBase{

   @Test
   public void testModifySWMgr_Success() {
      Mockito.when(appManagerService.findAll()).thenReturn(Arrays.asList(appManagerEntityFoo));
      Mockito.when(appManagerService.findAppManagerByName(Matchers.anyString())).thenReturn(appManagerEntityFoo);

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + appManagerEntityFoo.getType(), "com.vmware.bdd.manager.mocks.FooSWMgrFactory");
      softwareManagerCollector.setPrivateKey("mock-key");

      softwareManagerCollector.loadSoftwareManagers();

      AppManagerAdd appManagerAdd = SoftwareManagerCollector.toAppManagerAdd(appManagerEntityFoo);
      appManagerAdd.setName("newUserName");
      appManagerAdd.setPassword("newPassword");
      appManagerAdd.setUrl("http://newUrl");


      softwareManagerCollector.modifySoftwareManager(appManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Cannot modify default application manager.")
   public void testModifySWMgr_CantModifyDefault() {
      softwareManagerCollector.modifySoftwareManager(defaultAppManagerAdd);
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
   expectedExceptionsMessageRegExp = "Cannot find app manager fooAppMgr.")
   public void testModifySWMgr_NotFound() {
      softwareManagerCollector.modifySoftwareManager(appManagerAddFoo);
   }
}
