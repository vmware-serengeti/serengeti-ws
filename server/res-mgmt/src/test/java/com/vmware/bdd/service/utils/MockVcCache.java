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

import com.vmware.aurora.vc.*;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import mockit.Mock;
import mockit.MockClass;
import static org.mockito.Mockito.*;

@MockClass(realClass = VcCache.class)
public class MockVcCache {

    @Mock
    static public <T extends VcObject> T get(String id){
        VcVirtualMachine vm = mock(VcVirtualMachine.class);
        when(vm.getName()).thenReturn(id);
        ConfigInfo config = mock(ConfigInfo.class);
        when(vm.getConfig()).thenReturn(config);
        when(config.getUuid()).thenReturn("test-uuid");
        when(vm.getId()).thenReturn(id);
        VcDatacenter datacenter = mock(VcDatacenter.class);
        when(datacenter.getName()).thenReturn("DataCenter");
        when(datacenter.getName()).thenReturn("DataCenter");
        VcResourcePool resourcePool = mock(VcResourcePool.class);
        VcResourcePool parentResourcePool = mock(VcResourcePool.class);
        VcCluster cluster = mock(VcCluster.class);
        when(cluster.getDatacenter()).thenReturn(datacenter);
        when(resourcePool.getVcCluster()).thenReturn(cluster);
        when(vm.getParentVApp()).thenReturn(resourcePool);
        when(resourcePool.getParent()).thenReturn(parentResourcePool);
        when(parentResourcePool.getVcCluster()).thenReturn(cluster);
        return (T)vm;
    }
}
