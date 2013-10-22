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

/**
 * This class is the test of Network command.
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.IpAllocEntryRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.commands.NetworkCommands;

@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class NetworkCommandsTest extends MockRestServer {

   @Autowired
   private NetworkCommands networkCommands;

   @Test
   public void testPatten() {
      // Match a single IP format,eg. 192.168.0.2 .
      final String IP =
            "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";
      // Match a IP section format,eg. 192.168.0.2-100 .
      final String IPSEG =
            "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\-((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";

      Pattern ipPattern = Pattern.compile(IP);
      Pattern ipSegPattern = Pattern.compile(IPSEG);
      //Test a single IP format.
      assertEquals(ipPattern.matcher("192.168.0.1").matches(), true);
      assertEquals(ipPattern.matcher("88.207.6.12").matches(), true);
      assertEquals(ipPattern.matcher("abcd").matches(), false);
      assertEquals(ipPattern.matcher("256.0.0").matches(), false);
      assertEquals(ipPattern.matcher("256.0.0.1").matches(), false);
      //Test a IP section format.
      assertEquals(ipSegPattern.matcher("192.168.0.1-100").matches(), true);
      assertEquals(ipSegPattern.matcher("abcd").matches(), false);
      assertEquals(ipSegPattern.matcher("192.168.0.1").matches(), false);
      assertEquals(ipSegPattern.matcher("192.168.0.1-256").matches(), false);
      assertEquals(ipSegPattern.matcher("192.168.0-255.1-256").matches(), false);

      List<String> list = null;
      list = testIpGroupFormat("192.168.0.12,192.168.0.12,192.168.0.12-200");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), true);
      }
      list = testIpGroupFormat("192.168.0.12,192.168.0.12,192.168.0.12");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), true);
      }
      list = testIpGroupFormat("192.168.0.12-30,192.168.0.12-80,192.168.0.12-90");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), true);
      }

      list = testIpGroupFormat("192.168.0.12, ");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), true);
      }

      list = testIpGroupFormat("192.168.0.12,256.117.6.12-300");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         if (ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches())
            assertEquals(ipPattern.matcher(string).matches()
                  || ipSegPattern.matcher(string).matches(), true);
         else
            assertEquals(ipPattern.matcher(string).matches()
                  || ipSegPattern.matcher(string).matches(), false);
      }

      list = testIpGroupFormat(",");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), false);
      }

      list = testIpGroupFormat("123444");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), false);
      }

      list = testIpGroupFormat("aa,bb");
      for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
         String string = (String) iterator.next();
         assertEquals(ipPattern.matcher(string).matches()
               || ipSegPattern.matcher(string).matches(), false);
      }

   }

   @Test
   public void testAddNetwork() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.POST,
            HttpStatus.NO_CONTENT, "");
      networkCommands.addNetwork("name", "portGroup", false, "192.168.0.12",
            "192.168.0.13", "192.168.0.1,192.167.0.4-100", "192.168.1.1",
            "255.255.255.0");
      CookieCache.clear();
   }
   
   @Test
   public void testAddNetworkFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("already exists");
      ObjectMapper mapper = new ObjectMapper();
      
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.POST,
            HttpStatus.BAD_REQUEST, mapper.writeValueAsString(errorMsg));
      
      networkCommands.addNetwork("name", "portGroup", false, "192.168.0.12",
            "192.168.0.13", "192.168.0.1,192.168.3.4-100", "192.168.7.1",
            "255.255.255.0");
      CookieCache.clear();
   }

   @Test
   public void testModifyNetwork() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/network/staticNetwork", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      networkCommands.modifyNetwork("staticNetwork","192.168.0.2-100");
      CookieCache.clear();
   }

   @Test
   public void testDeleteNetwork() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/network/name",
            HttpMethod.DELETE, HttpStatus.NO_CONTENT, "");
      networkCommands.deleteNetwork("name");
      CookieCache.clear();
   }
   
   @Test
   public void testDeleteNetworkFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();
      
      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/network/name",
            HttpMethod.DELETE, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
      networkCommands.deleteNetwork("name");
      CookieCache.clear();
   }

   @Test
   public void testGetNetwork() {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      ObjectMapper mapper = new ObjectMapper();
      NetworkRead networkRead1 = new NetworkRead();
      networkRead1.setName("name1");
      networkRead1.setPortGroup("portGroup1");
      networkRead1.setDhcp(false);
      networkRead1.setDns1("192.1.1.1");
      networkRead1.setDns2("192.1.2.1");
      List<IpBlock> allIpBlocks = new ArrayList<IpBlock>();
      IpBlock ip1 = new IpBlock();
      ip1.setBeginIp("192.1.1.2");
      ip1.setEndIp("192.1.1.5");
      IpBlock ip2 = new IpBlock();
      ip2.setBeginIp("192.1.1.8");
      ip2.setEndIp("192.1.1.8");
      IpBlock ip3 = new IpBlock();
      ip3.setBeginIp("192.1.1.10");
      ip3.setEndIp("192.1.1.100");
      allIpBlocks.add(ip1);
      allIpBlocks.add(ip2);
      allIpBlocks.add(ip3);
      List<IpBlock> freeIpBlocks = new ArrayList<IpBlock>();
      freeIpBlocks.add(ip1);
      List<IpBlock> assignedIpBlocks = new ArrayList<IpBlock>();
      assignedIpBlocks.add(ip2);
      networkRead1.setAllIpBlocks(allIpBlocks);
      networkRead1.setFreeIpBlocks(freeIpBlocks);
      networkRead1.setAssignedIpBlocks(assignedIpBlocks);
      networkRead1.setGateway("192.1.1.0");
      networkRead1.setNetmask("255.255.0.0");
      List<IpAllocEntryRead> ipAllocEntries1 =
            new ArrayList<IpAllocEntryRead>();
      IpAllocEntryRead ipAllocEntry1 = new IpAllocEntryRead();
      ipAllocEntry1.setIpAddress("192.1.1.3");
      ipAllocEntry1.setNodeName("nodeName1");
      ipAllocEntry1.setNodeGroupName("nodeGroupName1");
      ipAllocEntry1.setClusterName("clusterName1");
      IpAllocEntryRead ipAllocEntry2 = new IpAllocEntryRead();
      ipAllocEntry2.setIpAddress("192.1.1.7");
      ipAllocEntry2.setNodeName("nodeName2");
      ipAllocEntry2.setNodeGroupName("nodeGroupName1");
      ipAllocEntry2.setClusterName("clusterName1");
      ipAllocEntries1.add(ipAllocEntry1);
      ipAllocEntries1.add(ipAllocEntry2);
      networkRead1.setIpAllocEntries(ipAllocEntries1);

      NetworkRead networkRead2 = new NetworkRead();
      networkRead2.setName("name2");
      networkRead2.setPortGroup("portGroup2");
      networkRead2.setDhcp(true);
      List<IpAllocEntryRead> ipAllocEntries2 =
            new ArrayList<IpAllocEntryRead>();
      IpAllocEntryRead ipAllocEntry3 = new IpAllocEntryRead();
      ipAllocEntry3.setIpAddress("192.1.10.3");
      ipAllocEntry3.setNodeName("nodeName3");
      ipAllocEntry3.setNodeGroupName("nodeGroupName2");
      ipAllocEntry3.setClusterName("clusterName2");
      IpAllocEntryRead ipAllocEntry4 = new IpAllocEntryRead();
      ipAllocEntry4.setIpAddress("192.1.10.7");
      ipAllocEntry4.setNodeName("nodeName4");
      ipAllocEntry4.setNodeGroupName("nodeGroupName2");
      ipAllocEntry4.setClusterName("clusterName2");
      ipAllocEntries2.add(ipAllocEntry3);
      ipAllocEntries2.add(ipAllocEntry4);
      networkRead2.setIpAllocEntries(ipAllocEntries2);
      getListNetwork(mapper, new NetworkRead[] { networkRead1, networkRead2 },
            true);
      CookieCache.clear();
   }

   private List<String> testIpGroupFormat(String str) {
      return CommandsUtils.inputsConvert(str);
   }

   private void getListNetwork(ObjectMapper mapper, NetworkRead[] networks,
         boolean detail) {
      try {
         if (detail) {
            buildReqRespWithoutReqBody(
                  "https://127.0.0.1:8443/serengeti/api/networks?details=true",
                  HttpMethod.GET, HttpStatus.OK,
                  mapper.writeValueAsString(networks));
            networkCommands.getNetwork(null, detail);
         } else {
            buildReqRespWithoutReqBody(
                  "https://127.0.0.1:8443/serengeti/api/networks",
                  HttpMethod.GET, HttpStatus.OK,
                  mapper.writeValueAsString(networks));
            networkCommands.getNetwork(null, detail);
         }

      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }

   @SuppressWarnings("unused")
   private void getNetworkByName(ObjectMapper mapper, NetworkRead network,
         boolean detail) {
      try {
         if (detail) {
            buildReqRespWithoutReqBody(
                  "https://127.0.0.1:8443/serengeti/api/network/name1?details=true",
                  HttpMethod.GET, HttpStatus.OK,
                  mapper.writeValueAsString(network));
            networkCommands.getNetwork("name1", detail);
         } else {
            buildReqRespWithoutReqBody(
                  "https://127.0.0.1:8443/serengeti/api/network/name1",
                  HttpMethod.GET, HttpStatus.OK,
                  mapper.writeValueAsString(network));
            networkCommands.getNetwork("name1", detail);
         }

      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }
   
   @Test
   public void testGetClusterFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody(
            "https://127.0.0.1:8443/serengeti/api/network/name1", HttpMethod.GET,
            HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
      networkCommands.getNetwork("name1", false);
      CookieCache.clear();
   }

}
