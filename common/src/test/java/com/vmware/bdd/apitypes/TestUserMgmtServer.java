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
package com.vmware.bdd.apitypes;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created By xiaoliangl on 12/24/14.
 */
public class TestUserMgmtServer {
   @Test
   public void test() throws IOException {
      ObjectMapper objectMapper = new ObjectMapper();
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            TestUserMgmtServer.class.getResourceAsStream("/com/vmware/bdd/apitypes/ldap-server.json"),
            UserMgmtServer.class);

      UserMgmtServer userMgmtServer1 = objectMapper.readValue(
            TestUserMgmtServer.class.getResourceAsStream("/com/vmware/bdd/apitypes/ldap-server.json"),
            UserMgmtServer.class);

      Assert.assertEquals(userMgmtServer, userMgmtServer1);
      Assert.assertEquals(userMgmtServer.hashCode(), userMgmtServer1.hashCode());
   }

   @Test
   public void test1() throws IOException {
      ObjectMapper objectMapper = new ObjectMapper();
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            TestUserMgmtServer.class.getResourceAsStream("/com/vmware/bdd/apitypes/ldap-server.json"),
            UserMgmtServer.class);

      UserMgmtServer userMgmtServer1 = objectMapper.readValue(
            TestUserMgmtServer.class.getResourceAsStream("/com/vmware/bdd/apitypes/ldaps-server.json"),
            UserMgmtServer.class);

      Assert.assertNotEquals(userMgmtServer, userMgmtServer1);
      Assert.assertNotEquals(userMgmtServer.hashCode(), userMgmtServer1.hashCode());
   }
}
