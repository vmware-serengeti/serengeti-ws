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

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.utils.VcVmUtil;

@MockClass(realClass = VcVmUtil.class)
public class MockVcVmUtil {
   private static boolean flag = true;
   private static int i = 0;

   @Mock
   public static String getIpAddress(final VcVirtualMachine vcVm,
         boolean inSession) {
      if (flag) {
         i++;
         return "10.1.1." + i;
      } else {
         return null;
      }
   }

   @Mock
   public static String getGuestHostName(final VcVirtualMachine vcVm,
         boolean inSession) {
      return "localhost";
   }

   public static void setFlag(boolean flag) {
      MockVcVmUtil.flag = flag;
   }

   @Mock
   public static boolean setBaseNodeForVm(BaseNode vNode, VcVirtualMachine vm) {
      return true;
   }

   @Mock
   public static boolean isDatastoreAccessible(String dsMobId) {
      // e.g., datastore:1000 return false
      if (dsMobId.endsWith("0"))
         return false;

      return true;
   }
   
   @Mock
   public static void updateVm(String vmId) {
	   
   }
}
