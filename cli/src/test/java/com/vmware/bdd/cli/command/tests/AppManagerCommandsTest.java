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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.cli.commands.AppManagerCommands;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.utils.Constants;

/**
 * This class is the test of appmanager command.
 */
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class AppManagerCommandsTest extends MockRestServer {
   @Autowired
   private AppManagerCommands appManagerCommands;

   private AppManagerRead defaultAmr;
   private AppManagerRead cmAmr;
   private AppManagerRead amAmr;

   @BeforeClass
   public void setUp() throws Exception {
      defaultAmr = new AppManagerRead();
      defaultAmr.setName(Constants.IRONFAN);
      cmAmr = new AppManagerRead();
      cmAmr.setName("cm");
      amAmr = new AppManagerRead();
      amAmr.setName("am");
   }

   /*@Test
   public void testAddAppManager() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanagers", HttpMethod.POST,
            HttpStatus.NO_CONTENT, "");


      InputStream stdin = System.in;
      ByteArrayInputStream input = new ByteArrayInputStream("admin\nadmin\n".getBytes());
      System.setIn(input);

      appManagerCommands.addAppManager("cm", "Cloudera Manager", "ClouderaManager", "http://10.20.30.40:7180");

      System.setIn(stdin);

      CookieCache.clear();
   }*/

   @Test
   public void testListAppManagers() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanagers",
            HttpMethod.GET,
            HttpStatus.OK,
            mapper.writeValueAsString(new AppManagerRead[] { defaultAmr, cmAmr,
                  amAmr }));

      // appmanager list
      appManagerCommands.listAppManager(null, false, null, false, false);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagerByName() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/cm",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(cmAmr));

      // appmanager list --name cm
      appManagerCommands.listAppManager("cm", false, null, false, false);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagerDistros() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/cm",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(cmAmr));
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/cm/distros",
            HttpMethod.GET, HttpStatus.OK, "");

      // appmanager list --name cm --distros
      appManagerCommands.listAppManager("cm", true, null, false, false);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagersDistros() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanagers",
            HttpMethod.GET,
            HttpStatus.OK,
            mapper.writeValueAsString(new AppManagerRead[] { defaultAmr, cmAmr,
                  amAmr }));
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/Default/distros",
            HttpMethod.GET, HttpStatus.OK, "");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/cm/distros",
            HttpMethod.GET, HttpStatus.OK, "");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/am/distros",
            HttpMethod.GET, HttpStatus.OK, "");

      // appmanager list --distros
      appManagerCommands.listAppManager(null, true, null, false, false);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagerRoles() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      //ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/Default/distro/bigtop/roles",
            HttpMethod.GET, HttpStatus.OK, "");

      // appmanager list --name Default --distro bigtop --roles
      appManagerCommands
            .listAppManager("Default", false, "bigtop", true, false);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagerConfigurations() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");

      //ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/Default/distro/bigtop/configurations",
            HttpMethod.GET, HttpStatus.OK, "");

      // appmanager list --name Default --distro apache --roles
      appManagerCommands
            .listAppManager("Default", false, "bigtop", false, true);

      CookieCache.clear();
   }

   @Test
   public void testListAppManagerFailure() throws Exception {
      CookieCache.put("Cookie", "JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/appmanager/mm",
            HttpMethod.GET, HttpStatus.NOT_FOUND,
            mapper.writeValueAsString(errorMsg));

      // appmanager list --name mm
      appManagerCommands.listAppManager("mm", false, null, false, false);

      CookieCache.clear();
   }

}
