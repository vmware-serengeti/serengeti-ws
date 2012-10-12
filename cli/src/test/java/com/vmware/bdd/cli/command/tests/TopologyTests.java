/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.RackInfo;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.commands.TopologyCommands;

@Test
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class TopologyTests extends MockRestServer {
   @Autowired
   private TopologyCommands topologyCommands;

   @Test
   public void testTopologyUpload() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      List<RackInfo> racksInfo = new ArrayList<RackInfo>();
      RackInfo rack1 = new RackInfo();
      rack1.setName("rack1");
      List<String> hosts1 = new ArrayList<String>();
      hosts1.add("host1");
      hosts1.add("host2");
      rack1.setHosts(hosts1);

      RackInfo rack2 = new RackInfo();
      rack2.setName("rack2");
      List<String> hosts2 = new ArrayList<String>();
      hosts2.add("host3");
      hosts2.add("host4");
      rack2.setHosts(hosts2);

      racksInfo.add(rack1);
      racksInfo.add(rack2);

      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/racks",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(racksInfo));
      buildReqRespWithoutRespBody("http://127.0.0.1:8080/serengeti/api/racks",
            HttpMethod.PUT, HttpStatus.NO_CONTENT, mapper.writeValueAsString(racksInfo));

      topologyCommands.upload("src/test/resources/topology.sample", true);
      CookieCache.clear();
   }

   @Test
   public void testTopologyList() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      RackInfo[] racksInfo = new RackInfo[2];
      RackInfo rack1 = new RackInfo();
      rack1.setName("Rack1");
      List<String> hosts1 = new ArrayList<String>();
      hosts1.add("host1");
      hosts1.add("host2");
      rack1.setHosts(hosts1);

      RackInfo rack2 = new RackInfo();
      rack2.setName("Rack2");
      List<String> hosts2 = new ArrayList<String>();
      hosts2.add("host3");
      hosts2.add("host4");
      rack2.setHosts(hosts2);

      racksInfo[0] = rack1;
      racksInfo[1] = rack2;

      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/racks",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(racksInfo));

      //get topology
      topologyCommands.list();
      CookieCache.clear();
   }
}