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
package com.vmware.bdd.service;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcClusterConfig;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.service.utils.VcResourceUtils;

@MockClass(realClass = VcResourceUtils.class)
public class MockVcResourceUtils {
   private static boolean flag = true;

   @Mock
   public static VcResourcePool findRPInVCCluster(final String clusterName,
         final String vcRPName) {
      if (flag) {
         return Mockito.mock(VcResourcePool.class);
      } else {
         return null;
      }
   }

   @Mock
   public static VcDatastore findDSInVcByName(String dsName) {
      if (flag) {
         return Mockito.mock(VcDatastore.class);
      } else {
         return null;
      }
   }

   @Mock
   public static VcCluster findVcCluster(final String clusterName) {
      if (flag) {
         VcCluster vcCluster = Mockito.mock(VcCluster.class);
         VcClusterConfig config = Mockito.mock(VcClusterConfig.class);
         Mockito.when(vcCluster.getConfig()).thenReturn(config);
         Mockito.when(config.getDRSEnabled()).thenReturn(true);
         return vcCluster;
      } else {
         return null;
      }
   }

   public static void setFlag(boolean flag) {
      MockVcResourceUtils.flag = flag;
   }

   @Mock
   public static VcHost findHost(final String hostName) {
      VcHost host = Mockito.mock(VcHost.class);
      Mockito.when(host.getName()).thenReturn("host1.eng.vmware.com");
      return host;
   }

   @Mock
   public static void refreshDatastore(final VcCluster c) {
      return;
   }

   @Mock
   public static void refreshResourcePool(final VcCluster c) {
      return;
   }
}
