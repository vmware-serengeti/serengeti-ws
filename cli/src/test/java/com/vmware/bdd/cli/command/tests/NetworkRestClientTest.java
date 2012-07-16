/******************************************************************************
 *       Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *      Licensed under the Apache License, Version 2.0 (the "License");
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

import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.shell.support.util.Assert;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.cli.rest.NetworkRestClient;

/**
 * This class is the test of Network rest client.
 */
@ContextConfiguration(locations = { "classpath:/META-INF/spring/spring-shell-plugin.xml" })
public class NetworkRestClientTest extends MockRestServer {
   @Autowired
   private NetworkRestClient networkRestClient;
   
   @Test
   public void add() throws Exception {
      Assert.notNull(networkRestClient);
      NetworkAdd networkAdd =new NetworkAdd();
      addByIP(networkAdd);
      addByDHCP(networkAdd);
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutRespBody(
            "http://127.0.0.1:8080/serengeti/api/networks", HttpMethod.POST,
            HttpStatus.NO_CONTENT, mapper.writeValueAsString(networkAdd));
      networkRestClient.add(networkAdd);
   }
   
   @Test
   public void delete() throws Exception {
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/network/name1", HttpMethod.DELETE, HttpStatus.NO_CONTENT, "");
      networkRestClient.delete("name1");
   }
   
   private void addByIP(NetworkAdd networkAdd){
      List<IpBlock> ips = new LinkedList<IpBlock>();
      IpBlock ipBlock = new IpBlock();
      ipBlock.setBeginIp("192.168.0.1");
      ipBlock.setEndIp("192.168.0.100");
      ips.add(ipBlock);
      networkAdd.setName("name");
      networkAdd.setPortGroup("portGroup");
      networkAdd.setIp(ips);
      networkAdd.setDns1("10.117.7.12");
      networkAdd.setGateway("10.117.7.1");
      networkAdd.setNetmask("255.255.255.0");
   }
   
   private void addByDHCP(NetworkAdd networkAdd){
      networkAdd.setName("name");
      networkAdd.setPortGroup("portGroup");
      networkAdd.setDhcp(true); 
   }
   
}
