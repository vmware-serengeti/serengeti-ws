/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.commands.ResourcePoolCommands;

@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class RpCommandsTest extends MockRestServer {
   @Autowired
   private ResourcePoolCommands rpCommands;

   @Test
   public void testCreateRp() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/resourcepools", HttpMethod.POST,
            HttpStatus.NO_CONTENT, "");

      rpCommands.addResourcePool("rp01", "vc_rp1", "vc_cluster1");
      CookieCache.clear();
   }
   
   @Test
   public void testCreateRpFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("already exists");
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/resourcepools", HttpMethod.POST,
            HttpStatus.BAD_REQUEST, mapper.writeValueAsString(errorMsg));

      rpCommands.addResourcePool("rp01", "vc_rp1", "vc_cluster1");
      CookieCache.clear();
   }

   @Test
   public void testDeleteRp() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/resourcepool/rp01", HttpMethod.DELETE,
            HttpStatus.NO_CONTENT, "");

      rpCommands.deleteResourcePool("rp01");
      CookieCache.clear();
   }
   
   @Test
   public void testDeleteRpFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/resourcepool/rp01", HttpMethod.DELETE,
            HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

      rpCommands.deleteResourcePool("rp01");
      CookieCache.clear();
   }
   
   @Test
   public void testRpList() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      ResourcePoolRead rp = new ResourcePoolRead();
      rp.setRpName("test01");
      rp.setVcCluster("home");
      rp.setRpVsphereName("hadoop01");
      rp.setTotalRAMInMB(8192);
      rp.setTotalCPUInMHz(2048);

      NodeRead node01 = new NodeRead();
      node01.setName("node01");
      node01.setIp("192.168.1.2");
      List<String> roles01 = new ArrayList<String>();
      roles01.add("NameNode");
      roles01.add("JobTracker");
      roles01.add("DataNode");
      roles01.add("HadoopClient");
      node01.setRoles(roles01);
      NodeRead node02 = new NodeRead();
      node02.setName("node02");
      node02.setIp("192.168.1.3");
      List<String> roles02 = new ArrayList<String>();
      roles02.add("NameNode");
      roles02.add("JobTracker");
      node02.setRoles(roles02);

      NodeRead[] nodes = { node01, node02 };

      rp.setNodes(nodes);
      
      ObjectMapper mapper = new ObjectMapper();
      this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/resourcepool/test01",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(rp));
    
      rpCommands.getResourcePool("test01", true);
      CookieCache.clear();
   }
   
   @Test
   public void testPrettyOutputResourcePoolsInfo() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      NodeRead node01 = new NodeRead();
      node01.setName("node01");
      node01.setIp("192.168.1.2");
      List<String> roles01 = new ArrayList<String>();
      roles01.add("NameNode");
      roles01.add("JobTracker");
      roles01.add("DataNode");
      roles01.add("HadoopClient");
      node01.setRoles(roles01);
      NodeRead node02 = new NodeRead();
      node02.setName("node02");
      node02.setIp("192.168.1.3");
      List<String> roles02 = new ArrayList<String>();
      roles02.add("NameNode");
      roles02.add("JobTracker");
      node02.setRoles(roles02);
      NodeRead[] nodes = { node01, node02 };

      ResourcePoolRead rp01 = new ResourcePoolRead();
      rp01.setRpName("test01");
      rp01.setVcCluster("home");
      rp01.setRpVsphereName("hadoop01");
      rp01.setTotalRAMInMB(8192);
      rp01.setTotalCPUInMHz(2048);
      rp01.setNodes(nodes);

      ResourcePoolRead rp02 = new ResourcePoolRead();
      rp02.setRpName("test02");
      rp02.setVcCluster("home");
      rp02.setRpVsphereName("hadoop02");
      rp02.setTotalRAMInMB(8192);
      rp02.setTotalCPUInMHz(2048);
      rp02.setNodes(nodes);

      ResourcePoolRead[] rps = { rp01, rp02 };
      ObjectMapper mapper = new ObjectMapper();
      this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/resourcepools",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(rps));
    
      rpCommands.getResourcePool(null, true);
      CookieCache.clear();
   }
   
   @Test
   public void testRpListFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();

      this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/resourcepool/rp1",
            HttpMethod.GET, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

      rpCommands.getResourcePool("rp1", true);
      CookieCache.clear();
   }
}
