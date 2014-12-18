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
package com.vmware.bdd.service.utils;

import java.util.Collection;
import java.util.List;

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;

public class TestVcResourceUtils {
   @BeforeClass(groups = {"TestVcResourceUtils"})
   public static void setup() {
      Mockit.setUpMock(MockVcInventory.class);
      Mockit.setUpMock(MockVcContext.class);
   }

   @AfterClass(groups = { "TestVcResourceUtils" })
   public static void tearDown() {
      Mockit.tearDownMocks();
   }

   @Test
   public void testFindDSInVCByPattern() {
      Collection<VcDatastore> dss = VcResourceUtils.findDSInVCByPattern("line.*");
      Assert.assertNotNull(dss);
      dss = VcResourceUtils.findDSInVCByPattern("test.*");
      Assert.assertEquals(dss.size(), 1);
   }

   @Test
   public void testFindDSInVcByName() {
      VcDatastore ds = VcResourceUtils.findDSInVcByName("line_1");
      Assert.assertNull(ds);
      ds = VcResourceUtils.findDSInVcByName("test_1");
      Assert.assertEquals(ds.getName(), "test_1");
   }

   @Test
   public void testFindNetworkInVC() {
      VcNetwork net = VcResourceUtils.findNetworkInVC("port2");
      Assert.assertNull(net);
      net = VcResourceUtils.findNetworkInVC("port1");
      Assert.assertNotNull(net);
   }

   @Test
   public void testFindAllHostsInVCCluster() {
      List<VcHost> hosts = VcResourceUtils.findAllHostsInVCCluster("cluster2");
      Assert.assertTrue(hosts.isEmpty());
      hosts = VcResourceUtils.findAllHostsInVCCluster("cluster1");
      Assert.assertTrue(hosts.size() > 0);
   }

   @Test
   public void testFindAllHostInVcResourcePool() {
      List<VcHost> host = VcResourceUtils.findAllHostInVcResourcePool("cluster1", "rp1");
      Assert.assertEquals(host.size(), 1);
      host = VcResourceUtils.findAllHostInVcResourcePool("cluster2", "rp2");
      Assert.assertEquals(host.size(), 0);
   }

   @Test
   public void testFindRPInVCCluster() {
      VcResourcePool rp = VcResourceUtils.findRPInVCCluster("cluster2", "rp1");
      Assert.assertNull(rp);
      rp = VcResourceUtils.findRPInVCCluster("cluster1", "rp2");
      Assert.assertNull(rp);
      rp = VcResourceUtils.findRPInVCCluster("cluster1", "rp1");
      Assert.assertNotNull(rp);
   }

   @Test
   public void testFindVmInVcCluster() {
      VcVirtualMachine vm = VcResourceUtils.findVmInVcCluster("cluster1", "rp1", "vm2");
      Assert.assertNull(vm);
      vm = VcResourceUtils.findVmInVcCluster("cluster1", "rp1", "vm1");
      Assert.assertNotNull(vm);
   }
}
