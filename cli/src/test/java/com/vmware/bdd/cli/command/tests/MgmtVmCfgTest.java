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
package com.vmware.bdd.cli.command.tests;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.commands.MgmtVmCfgCommands;
import com.vmware.bdd.usermgmt.UserMgmtConstants;

/**
 * Created By xiaoliangl on 5/26/15.
 */
@Test
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class MgmtVmCfgTest extends MockRestServer {

   private ObjectMapper objectMapper = new ObjectMapper();
   @Autowired
   private MgmtVmCfgCommands mgmtVmCfgCommands;


   @Test
   public void testGetMgmtVMCfg() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      Map<String, String> mgmtVMcfg = new HashMap<>();
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE, "LOCAL");

      this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/vmconfig/mgmtvm/",
            HttpMethod.GET, HttpStatus.OK,
            objectMapper.writeValueAsString(mgmtVMcfg));

      mgmtVmCfgCommands.getMgmtVMCfg();
   }

   @Test
   public void testModify() throws JsonProcessingException {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      String mode = "LOCAL";

      Map<String, Object> mgmtVMcfg = new HashMap<>();
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE, mode);
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME, UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

      this.buildReqRespWithoutRespBody("https://127.0.0.1:8443/serengeti/api/vmconfig/mgmtvm",
            HttpMethod.PUT, HttpStatus.NO_CONTENT,
            objectMapper.writeValueAsString(mgmtVMcfg));

      mgmtVmCfgCommands.modifyMgmtVMCfg(mode);
   }

   @Test
   public void testIsCommandAvailable() {
      Assert.assertTrue(mgmtVmCfgCommands.isCommandAvailable());
   }

}
