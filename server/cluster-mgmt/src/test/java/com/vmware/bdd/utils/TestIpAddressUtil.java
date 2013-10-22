/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.utils;

import static com.vmware.bdd.utils.IpAddressUtil.getAddressAsLong;
import static com.vmware.bdd.utils.IpAddressUtil.getNetworkPrefixBits;
import static com.vmware.bdd.utils.IpAddressUtil.isValidIp;
import static com.vmware.bdd.utils.IpAddressUtil.isValidNetmask;
import static com.vmware.bdd.utils.IpAddressUtil.networkContains;
import static com.vmware.bdd.utils.IpAddressUtil.verifyIPBlocks;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mchange.util.AssertException;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.exception.BddException;

public class TestIpAddressUtil {
   @BeforeMethod
   public void setup() {

   }

   @AfterMethod
   public void tearDown() {

   }

   @AfterClass
   public static void deleteAll() {
   }

   private Long convertIp(String addr) throws UnknownHostException {
      return IpAddressUtil.getAddressAsLong(InetAddress.getByName(addr));
   }

   @Test
   public void testGetNetworkPrefixBits() throws UnknownHostException {
      assertEquals(32, getNetworkPrefixBits(convertIp("255.255.255.255")));
      assertEquals(30, getNetworkPrefixBits(convertIp("255.255.255.252")));
      assertEquals(24, getNetworkPrefixBits(convertIp("255.255.255.0")));
      assertEquals(16, getNetworkPrefixBits(convertIp("255.255.0.0")));
      assertEquals(8, getNetworkPrefixBits(convertIp("255.0.0.0")));
      assertEquals(1, getNetworkPrefixBits(convertIp("128.0.0.0")));
      assertEquals(3, getNetworkPrefixBits(convertIp((128 + 64 + 32) + ".0.0.0")));

      assertEquals(-1, getNetworkPrefixBits(convertIp("0.0.0.0")));
      assertEquals(-1, getNetworkPrefixBits(convertIp((128 + 64 + 16) + ".0.0.0")));
   }

   @Test
   public void testIsNetmaskValid() throws UnknownHostException {
      assertTrue(isValidNetmask("255.255.255.255"));
      assertTrue(isValidNetmask("255.255.255.252"));
      assertTrue(isValidNetmask("255.255.255.0"));
      assertTrue(isValidNetmask("255.255.0.0"));
      assertTrue(isValidNetmask("255.0.0.0"));
      assertTrue(isValidNetmask("128.0.0.0"));
      assertTrue(isValidNetmask((128 + 64 + 32) + ".0.0.0"));

      assertFalse(isValidNetmask("0.0.0.0"));
      assertFalse(isValidNetmask((128 + 64 + 16) + ".0.0.0"));
   }

   @Test
   public void testNetworkContains() throws UnknownHostException {
      assertTrue(networkContains(convertIp("192.168.1.0"),
            convertIp("255.255.255.0"), convertIp("192.168.1.1")));

      assertTrue(networkContains(convertIp("192.168.1.0"),
            convertIp("255.255.255.0"), convertIp("192.168.1.255")));

      assertTrue(networkContains(convertIp("192.168.1.0"),
            convertIp("255.255.255.0"), convertIp("192.168.1.128")));

      assertFalse(networkContains(convertIp("192.168.1.0"),
            convertIp("255.255.255.0"), convertIp("192.168.2.1")));
   }

   @Test
   public void testGetAddressAsLong() throws UnknownHostException {
      assertEquals(getAddressAsLong("192.168.1.0"), convertIp("192.168.1.0"));
      assertEquals(getAddressAsLong("255.255.255.0"), convertIp("255.255.255.0"));
      assertEquals(getAddressAsLong("192.168.1.128"), convertIp("192.168.1.128"));
      assertEquals(getAddressAsLong("0.0.0.0"), convertIp("0.0.0.0"));
      assertEquals(getAddressAsLong("255.255.255.255"), convertIp("255.255.255.255"));

      assertNull(getAddressAsLong("a.b.c.d"));
      assertNull(getAddressAsLong("256.0.0.0"));
      assertNull(getAddressAsLong("-1.0.0.0"));
      assertNull(getAddressAsLong("1.-1.0.0"));
      assertNull(getAddressAsLong("1..0.0"));
      assertNull(getAddressAsLong("1.0.0"));
      assertNull(getAddressAsLong(""));
   }

   @Test
   public void testIsIpValid() throws UnknownHostException {
      assertTrue(isValidIp("255.255.255.0", "192.168.0.1"));
      assertTrue(isValidIp("255.255.255.0", "192.168.0.254"));
      assertFalse(isValidIp("255.255.255.0", "192.168.0.0"));
      assertFalse(isValidIp("255.255.255.0", "192.168.0.255"));

      assertTrue(isValidIp("255.255.255.128", "192.168.0.129"));
      assertTrue(isValidIp("255.255.255.128", "192.168.0.254"));
      assertFalse(isValidIp("255.255.255.128", "192.168.0.128"));
      assertFalse(isValidIp("255.255.255.128", "192.168.0.255"));

      assertTrue(isValidIp("255.0.0.0", "10.0.0.1"));
      assertTrue(isValidIp("255.0.0.0", "10.255.255.254"));
      assertFalse(isValidIp("255.0.0.0", "10.0.0.0"));
      assertFalse(isValidIp("255.0.0.0", "10.255.255.255"));
   }

   @Test
   public void testVerifyIPBlocks() {
      long netmask = IpAddressUtil.getAddressAsLong("255.255.254.0");
      List<IpBlock> ipBlocks = new ArrayList<IpBlock>();
      ipBlocks.add(new IpBlock("192.168.1.11", "192.168.1.12"));
      verifyIPBlocks(ipBlocks, netmask);
      ipBlocks.clear();
      ipBlocks.add(new IpBlock("", "192.168.1.12"));
      try {
         verifyIPBlocks(ipBlocks, netmask);
      } catch (BddException e) {
         assertEquals(e.getMessage(),
               "Invalid value: IP block=[, 192.168.1.12].");
      }
      ipBlocks.clear();
      ipBlocks.add(new IpBlock("192.168.1.11", ""));
      try {
         verifyIPBlocks(ipBlocks, netmask);
      } catch (BddException e) {
         assertEquals(e.getMessage(),
               "Invalid value: IP block=[192.168.1.11, ].");
      }
      ipBlocks.clear();
      ipBlocks.add(new IpBlock("192.168.1.12", "192.168.1.11"));
      try {
         verifyIPBlocks(ipBlocks, netmask);
      } catch (BddException e) {
         assertEquals(e.getMessage(),
               "Invalid value: IP block=[192.168.1.12, 192.168.1.11].");
      }
   }

}
