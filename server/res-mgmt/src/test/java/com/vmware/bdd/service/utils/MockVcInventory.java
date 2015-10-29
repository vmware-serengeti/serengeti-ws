/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;

@MockClass(realClass = VcInventory.class)
public class MockVcInventory {
   @Mock
   static public List<VcCluster> getClusters() {
      List<VcCluster> clusters = prepareClusters();
      return clusters;
   }

   @Mock
   public static void loadInventory() {}

   @Mock
   static public List<VcCluster> getClustersInDatacenter(String datacenterName) {
      List<VcCluster> clusters = prepareClusters();
      return clusters;
   }

   private static List<VcCluster> prepareClusters() {
      VcCluster cluster1 = Mockito.mock(VcCluster.class);
      VcDatastore ds1 = Mockito.mock(VcDatastore.class);
      Mockito.when(ds1.getName()).thenReturn("test_1");
      List<VcDatastore> dsList1 = new ArrayList<VcDatastore>();
      dsList1.add(ds1);
      Mockito.when(cluster1.getAllDatastores()).thenReturn(dsList1);
      List<VcCluster> clusters = new ArrayList<VcCluster>();
      clusters.add(cluster1);
      Mockito.when(cluster1.getDatastore("test_1")).thenReturn(ds1);

      List<VcNetwork> nets = new ArrayList<VcNetwork>();
      VcNetwork net1 = Mockito.mock(VcNetwork.class);
      Mockito.when(net1.getName()).thenReturn("port1");
      nets.add(net1);
      Mockito.when(cluster1.getAllNetworks()).thenReturn(nets);

      Mockito.when(cluster1.getName()).thenReturn("cluster1");
      List<VcHost> hosts = new ArrayList<VcHost>();
      VcHost host1 = Mockito.mock(VcHost.class);
      hosts.add(host1);
      try {
         Mockito.when(cluster1.getHosts()).thenReturn(hosts);

         VcResourcePool rp1 = Mockito.mock(VcResourcePool.class);
         Mockito.when(cluster1.searchRP("[cluster1]/rp1")).thenReturn(rp1);
         VcResourcePool clusterRP = Mockito.mock(VcResourcePool.class);
         Mockito.when(cluster1.searchRP("[cluster1]")).thenReturn(clusterRP);

         List<VcVirtualMachine> vms = new ArrayList<VcVirtualMachine>();
         VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
         vms.add(vm);
         Mockito.when(rp1.getChildVMs()).thenReturn(vms);
         Mockito.when(vm.getName()).thenReturn("vm1");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      return clusters;
   }
}
