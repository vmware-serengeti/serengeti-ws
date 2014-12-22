/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.collection.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.vim.binding.vim.vm.ConfigInfo;

@MockClass(realClass = VcCache.class)
public class MockVcCache {
   private static Map<String, VcVirtualMachine> cache = new HashMap<String, VcVirtualMachine>();

   public static void cleanCache() {
      cache.clear();
   }

   @Mock
   static public <T extends VcObject> T get(String id) {
      return getIgnoreMissing(id);
   }

   @Mock
   static public <T extends VcObject> T getIgnoreMissing(String id) {
      if (cache.containsKey(id)) {
         return (T)cache.get(id);
      }
      if (id != null && id.contains("VirtualMachine")) {
         VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
         Mockito.when(vm.getName()).thenReturn(id);
         ConfigInfo config = Mockito.mock(ConfigInfo.class);
         Mockito.when(vm.getConfig()).thenReturn(config);
         Mockito.when(config.getUuid()).thenReturn("test-uuid");
         Mockito.when(vm.getId()).thenReturn(id);
         List<VcCluster> vcClusters = new ArrayList<>();
         VcCluster vcCluster = Mockito.mock(VcCluster.class);
         Mockito.when(vcCluster.getTotalCpu()).thenReturn(10000);
         Mockito.when(vcCluster.getTotalMemory()).thenReturn(1048576000L);
         Mockito.when(vcCluster.getNumberOfHost()).thenReturn(9);
         vcClusters.add(vcCluster);
         VcDatacenter vcDatacenter = Mockito.mock(VcDatacenter.class);
         Mockito.when(vcDatacenter.getVcClusters()).thenReturn(vcClusters);
         Mockito.when(vm.getDatacenter()).thenReturn(vcDatacenter);
         VcHost vcHost1 = Mockito.mock(VcHost.class);
         Mockito.when(vcHost1.getName()).thenReturn("192.168.0.1");
         Mockito.when(vm.getHost()).thenReturn(vcHost1);
         VcHost vcHost2 = Mockito.mock(VcHost.class);
         Mockito.when(vcHost2.getName()).thenReturn("192.168.0.2");
         Mockito.when(vm.getHost()).thenReturn(vcHost2);
         List<VcDatastore> vcDataStores = new ArrayList <>();
         VcDatastore vcDatastore1 = Mockito.mock(VcDatastore.class);
         VcDatastore vcDatastore2 = Mockito.mock(VcDatastore.class);
         vcDataStores.add(vcDatastore1);
         vcDataStores.add(vcDatastore2);
         List<VcHost> vcHosts1 = new ArrayList<>();
         List<VcHost> vcHosts2 = new ArrayList<>();
         vcHosts1.add(vcHost1);
         Mockito.when(vcDatastore1.getHosts()).thenReturn(vcHosts1);
         vcHosts2.add(vcHost1);
         vcHosts2.add(vcHost2);
         Mockito.when(vcDatastore2.getHosts()).thenReturn(vcHosts2);
         Mockito.when(vcCluster.getAllDatastores()).thenReturn(vcDataStores);
         cache.put(id, vm);
         return (T)vm;
      } else {
         return null;
      }
   }

}
