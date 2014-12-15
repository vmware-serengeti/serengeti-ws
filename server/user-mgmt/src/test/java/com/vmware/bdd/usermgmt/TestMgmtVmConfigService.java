/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.usermgmt;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.gson.Gson;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.usermgmt.job.MgmtVmConfigJobService;
import com.vmware.bdd.usermgmt.persist.MgmtVmCfgEao;

/**
 * Created By xiaoliangl on 12/3/14.
 */
public class TestMgmtVmConfigService  {

   @InjectMocks
   private MgmtVmCfgService mgmtVmCfgService;

   @Mock
   private MgmtVmConfigJobService mockMgmtVmConfigJobService;

   @Mock
   private JobManager mockJobManager;

   @Mock
   public UserMgmtServerService mockUserMgmtServerService;

   @BeforeMethod
   public void init() {
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public  void testCfgMixedMode() {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());

      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgMixed.json");

      Map<String,String> mgmtVmCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      MgmtVmCfgEao mockMgmtVmCfgEao = new MgmtVmCfgEao();
      mgmtVmCfgService.setMgmtVmCfgEao(mockMgmtVmCfgEao);

      mgmtVmCfgService.config(mgmtVmCfg);

      Map<String,String> mgmtVmCfg1 = mgmtVmCfgService.get();

      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
   }

   @Test
   public  void testCfgLdapMode() {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());

      testCfgMixedMode();
      Map<String,String> initCfg = mgmtVmCfgService.get();

      Gson gson = new Gson();
      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgLdap.json");

      Map<String,String> newCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      mgmtVmCfgService.config(newCfg);

      Map<String,String> finalCfg = mgmtVmCfgService.get();

      Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), newCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), initCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
   }

   @Test(expectedExceptions = {BddException.class})
   public  void testCfgLdapMode_NotAllowed() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgLdap.json");

      Map<String,String> mgmtVmCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      MgmtVmCfgEao mockMgmtVmCfgEao = new MgmtVmCfgEao();
      mgmtVmCfgService.setMgmtVmCfgEao(mockMgmtVmCfgEao);
      Map<String,String> initCfg = mgmtVmCfgService.get();

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (BddException bdde) {
         Map<String,String> finalCfg = mgmtVmCfgService.get();

         Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), initCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
         Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), initCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));

         throw bdde;
      }

   }

   @Test(expectedExceptions = {ValidationException.class})
   public  void testCfgMixedMode_WrongUserMgmtServerName() {
      Mockito.when(mockUserMgmtServerService.getByName("not_default", false)).thenReturn(new UserMgmtServer());

      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgMixed.json");

      Map<String,String> mgmtVmCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      MgmtVmCfgEao mockMgmtVmCfgEao = new MgmtVmCfgEao();
      mgmtVmCfgService.setMgmtVmCfgEao(mockMgmtVmCfgEao);

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (ValidationException ve) {

         Assert.assertNotNull(ve.getErrors().get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public  void testCfgMixedMode_EmptyUserMgmtServerName() {
      Mockito.when(mockUserMgmtServerService.getByName("not_default", false)).thenReturn(new UserMgmtServer());

      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgMixed-emptyname.json");

      Map<String,String> mgmtVmCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      MgmtVmCfgEao mockMgmtVmCfgEao = new MgmtVmCfgEao();
      mgmtVmCfgService.setMgmtVmCfgEao(mockMgmtVmCfgEao);

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (ValidationException ve) {

         Assert.assertNotNull(ve.getErrors().get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
         throw ve;
      }
   }

   @Test
   public  void testCfgMixedModeAgain() {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());

      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/mgmtvm-cfgMixed.json");

      Map<String,String> mgmtVmCfg = gson.fromJson(new InputStreamReader(ris), Map.class);

      MgmtVmCfgEao mockMgmtVmCfgEao = new MgmtVmCfgEao();
      mgmtVmCfgService.setMgmtVmCfgEao(mockMgmtVmCfgEao);

      mgmtVmCfgService.config(mgmtVmCfg);
      mgmtVmCfgService.config(mgmtVmCfg);

      Map<String,String> mgmtVmCfg1 = mgmtVmCfgService.get();

      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
   }
}
