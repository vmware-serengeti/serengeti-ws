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
package com.vmware.bdd.service;

import java.util.HashMap;
import java.util.Map;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.service.job.software.TestPreConfigAop;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@MockClass(realClass = VcCache.class)
public class MockVcCache {

   private static boolean getFlag = false;
   private static Map<String, VcVirtualMachine> cache = new HashMap<String, VcVirtualMachine>();

   public static void cleanCache() {
      cache.clear();
   }

   @Mock
   static public <T extends VcObject> T getIgnoreMissing(String id) {
      if (cache.containsKey(id)) {
         return (T)cache.get(id);
      }
      if (id != null && id.contains(TestPreConfigAop.VM_MOB_PREFIX)) {
         VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
         Mockito.when(vm.getName()).thenReturn(id);
         ConfigInfo config = Mockito.mock(ConfigInfo.class);
         Mockito.when(vm.getConfig()).thenReturn(config);
         Mockito.when(config.getUuid()).thenReturn("test-uuid");
         Mockito.when(vm.getId()).thenReturn(id);
         cache.put(id, vm);
         return (T)vm;
      } else if (getFlag) {
         if (id != null && id.equals("create-vm-succ")) {
            VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
            VcHost host = Mockito.mock(VcHost.class);
            Mockito.when(vm.getHost()).thenReturn(host);
            Mockito.when(host.getName()).thenReturn("host1.eng.vmware.com");
            VcResourcePool rp = Mockito.mock(VcResourcePool.class);
            Mockito.when(vm.getResourcePool()).thenReturn(rp);
            VcCluster cluster = Mockito.mock(VcCluster.class);
            Mockito.when(rp.getVcCluster()).thenReturn(cluster);
            Mockito.when(cluster.getName()).thenReturn("cluster-ws");
            return (T)vm;
         } else {
            VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
            Mockito.when(vm.getName()).thenReturn(id);
            ConfigInfo config = Mockito.mock(ConfigInfo.class);
            Mockito.when(vm.getConfig()).thenReturn(config);
            Mockito.when(config.getUuid()).thenReturn("test-uuid");
            Mockito.when(vm.getId()).thenReturn(id);
            return (T)vm;
         }
      } else {
         return null;
      }
   }

   public static void setGetFlag(boolean flag) {
      getFlag = flag;
   }

   @Mock
   static public <T extends VcObject> T getIgnoreMissing(ManagedObjectReference moRef) {
      if (getFlag) {
         VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
         String vmName = moRef.getValue();
         Mockito.when(vm.getName()).thenReturn(vmName);
         return (T)vm;
      } else {
         return null;
      }
   }
}
