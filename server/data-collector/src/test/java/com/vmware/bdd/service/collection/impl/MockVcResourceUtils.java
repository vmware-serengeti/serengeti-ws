/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
import java.util.List;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.ResourcePool.ResourceUsage;

@MockClass(realClass = VcResourceUtils.class)
public class MockVcResourceUtils {

   @Mock
   public static VcResourcePool findRPInVCCluster(final String clusterName,
         final String vcRPName) {
      VcResourcePool rp = Mockito.mock(VcResourcePool.class);
      ResourceAllocationInfo cpuResourceAllocationInfo =
            Mockito.mock(ResourceAllocationInfo.class);
      Mockito.when(cpuResourceAllocationInfo.getLimit()).thenReturn(-1L);
      ResourceAllocationInfo memResourceAllocationInfo =
            Mockito.mock(ResourceAllocationInfo.class);
      Mockito.when(memResourceAllocationInfo.getLimit()).thenReturn(-1L);
      ResourceUsage cpuResourceUsage = Mockito.mock(ResourceUsage.class);
      Mockito.when(cpuResourceUsage.getMaxUsage()).thenReturn(10000L);
      ResourceUsage memResourceUsage = Mockito.mock(ResourceUsage.class);
      Mockito.when(memResourceUsage.getMaxUsage()).thenReturn(1048576000L);
      Mockito.when(rp.getCpuAllocationInfo()).thenReturn(
            cpuResourceAllocationInfo);
      Mockito.when(rp.getCpuUsageInfo()).thenReturn(cpuResourceUsage);
      Mockito.when(rp.getMemAllocationInfo()).thenReturn(
            memResourceAllocationInfo);
      Mockito.when(rp.getMemUsageInfo()).thenReturn(memResourceUsage);
      Mockito.when(rp.isRootRP()).thenReturn(false);
      return rp;
   }

   @Mock
   public static List<VcHost> findAllHostsInVCCluster(final String clusterName) {
      List<VcHost> vcHosts = new ArrayList<>();
      VcHost host1 = Mockito.mock(VcHost.class);
      Mockito.when(host1.getVersion()).thenReturn("5.5.0");
      VcHost host2 = Mockito.mock(VcHost.class);
      Mockito.when(host2.getVersion()).thenReturn("5.1.0");
      vcHosts.add(host1);
      vcHosts.add(host2);
      return vcHosts;
   }
}
