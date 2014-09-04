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

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.mocks.FooSWMgrFactory;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 9/4/14.
 */
public class TestSWMgrCollectorBase {
   protected static AppManagerAdd defaultAppManagerAdd = null;
   protected static AppManagerEntity defaultAppManagerEntity = null;
   protected static AppManagerRead defaultAppManagerRead = null;

   protected static AppManagerAdd appManagerAddFoo;
   protected static AppManagerEntity appManagerEntityFoo = null;

   protected IAppManagerService appManagerService;
   protected SoftwareManagerCollector softwareManagerCollector;
   protected IClusterEntityManager clusterEntityManager;

   @BeforeMethod
   public void setUp() {
      softwareManagerCollector = new SoftwareManagerCollector();

      appManagerService = Mockito.mock(IAppManagerService.class);
      clusterEntityManager = Mockito.mock(IClusterEntityManager.class);

      softwareManagerCollector.setAppManagerService(appManagerService);
      softwareManagerCollector.setClusterEntityManager(clusterEntityManager);

      Configuration.setString(SoftwareManagerCollector.configurationPrefix + Constants.IRONFAN, "");
      Configuration.setString(SoftwareManagerCollector.configurationPrefix + FooSWMgrFactory.FOO_APP_MGR, "");
   }

   static {
      defaultAppManagerAdd = new AppManagerAdd();
      defaultAppManagerAdd.setName(Constants.IRONFAN);
      defaultAppManagerAdd.setDescription(Constants.IRONFAN_DESCRIPTION);
      defaultAppManagerAdd.setType(Constants.IRONFAN);
      defaultAppManagerAdd.setUrl("http://address");
      defaultAppManagerAdd.setUsername("");
      defaultAppManagerAdd.setPassword("");
      defaultAppManagerAdd.setSslCertificate("");

      defaultAppManagerEntity = new AppManagerEntity(defaultAppManagerAdd);

      defaultAppManagerRead = new AppManagerRead();
      defaultAppManagerRead.setName(Constants.IRONFAN);
      defaultAppManagerRead.setDescription(Constants.IRONFAN_DESCRIPTION);
      defaultAppManagerRead.setType(Constants.IRONFAN);
      defaultAppManagerRead.setUrl("http://address");
      defaultAppManagerRead.setUsername("");
      defaultAppManagerRead.setPassword("");
      defaultAppManagerRead.setSslCertificate("");

      appManagerAddFoo = new AppManagerAdd();
      appManagerAddFoo.setName("fooAppMgr");
      appManagerAddFoo.setDescription("fooAppMgr");
      appManagerAddFoo.setType("fooAppMgr");
      appManagerAddFoo.setUrl("http://address");
      appManagerAddFoo.setUsername("");
      appManagerAddFoo.setPassword("");
      appManagerAddFoo.setSslCertificate("");

      appManagerEntityFoo = new AppManagerEntity(appManagerAddFoo);
   }
}
