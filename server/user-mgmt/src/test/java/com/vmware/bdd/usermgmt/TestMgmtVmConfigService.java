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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.usermgmt.job.MgmtVmConfigJobService;
import com.vmware.bdd.usermgmt.mocks.MgmtVmCfgEaoMock;
import com.vmware.bdd.usermgmt.persist.MgmtVmCfgEao;

/**
 * Created By xiaoliangl on 12/3/14.
 */
public class TestMgmtVmConfigService {

   @InjectMocks
   private MgmtVmCfgService mgmtVmCfgService;

   @Mock
   private MgmtVmConfigJobService mockMgmtVmConfigJobService;

//   @Mock
//   private JobManager mockJobManager;

   @Mock
   public UserMgmtServerService mockUserMgmtServerService;

   @BeforeMethod
   public void init() {
      MockitoAnnotations.initMocks(this);

      HashMap<String, String> cfgMap = new HashMap<>();
      cfgMap.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE, UserMgmtMode.LOCAL.name());
      MgmtVmCfgEaoMock mgmtVmCfgEaoMock = new MgmtVmCfgEaoMock(cfgMap);

      mgmtVmCfgService.setMgmtVmCfgEao(mgmtVmCfgEaoMock);
   }

   @AfterMethod
   public void clean() {

   }

   private final static Map<String, String> loadTestData(String fileName) throws IOException {
      ObjectMapper objectMapper = new ObjectMapper();
      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/" + fileName);
      return objectMapper.readValue(new InputStreamReader(ris), Map.class);
   }


   @Test
   public  void testCfgMixedMode() throws IOException {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());
      Map<String,String> mgmtVmCfg = loadTestData("mgmtvm-cfgMixed.json");

      mgmtVmCfgService.config(mgmtVmCfg);

      Map<String,String> mgmtVmCfg1 = mgmtVmCfgService.get();

      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      Assert.assertEquals(mgmtVmCfg1.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), mgmtVmCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
   }

   @Test
   public  void testCfgLdapMode() throws IOException {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());

      testCfgMixedMode();
      Map<String,String> initCfg = mgmtVmCfgService.get();

      Map<String,String> newCfg = loadTestData("mgmtvm-cfgLDAP.json");

      mgmtVmCfgService.config(newCfg);

      Map<String,String> finalCfg = mgmtVmCfgService.get();

      Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE), newCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      Assert.assertEquals(finalCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME), initCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
   }

   @Test(expectedExceptions = {BddException.class})
   public  void testCfgLdapMode_NotAllowed() throws IOException {
      Map<String,String> mgmtVmCfg = loadTestData("mgmtvm-cfgLDAP.json");

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
   public  void testCfgMixedMode_WrongUserMgmtServerName() throws IOException {
      Mockito.when(mockUserMgmtServerService.getByName("not_default", false)).thenReturn(new UserMgmtServer());

      Map<String,String> mgmtVmCfg = loadTestData("mgmtvm-cfgMixed.json");

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (ValidationException ve) {

         Assert.assertNotNull(ve.getErrors().get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public  void testCfgMixedMode_EmptyUserMgmtServerName() throws IOException {
      Mockito.when(mockUserMgmtServerService.getByName("not_default", false)).thenReturn(new UserMgmtServer());

      Map<String,String> mgmtVmCfg = loadTestData("mgmtvm-cfgMixed-emptyname.json");

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (ValidationException ve) {

         Assert.assertNotNull(ve.getErrors().get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME));
         throw ve;
      }
   }

   @Test(expectedExceptions = {BddException.class})
   public  void testCfgMixedModeAgain() throws IOException {
      Mockito.when(mockUserMgmtServerService.getByName("default", false)).thenReturn(new UserMgmtServer());

      Map<String,String> mgmtVmCfg = loadTestData("mgmtvm-cfgMixed.json");

      mgmtVmCfgService.config(mgmtVmCfg);

      try {
         mgmtVmCfgService.config(mgmtVmCfg);
      } catch (BddException bdde) {
         Assert.assertEquals("MGMTVM_CUM_CFG.ALREADY_IN_TARGET_MODE", bdde.getFullErrorId());
         throw bdde;
      }
   }
}
