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
package com.vmware.bdd.software.mgmt.plugin.impl;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.plugin.ironfan.impl.DistroManager;
import com.vmware.bdd.utils.Constants;

public class TestDistroManager {

   public void testGetDistrosFailure() {
      List<String> vendors = new ArrayList<String>();
      try {
         DistroManager distroManager = new DistroManager();
         List<DistroRead> distros = distroManager.getDistros();
         for (DistroRead dr : distros) {
            vendors.add(dr.getVendor());
         }
         fail("creating cluster");
      } catch (Exception e) {

      }
      assertTrue("vendors must be empty.", vendors.isEmpty());
   }

   @Test
   public void testGetDistros() {
      List<String> vendors = new ArrayList<String>();
      DistroManager distroManager = new DistroManager();
      List<DistroRead> distros = distroManager.getDistros();
      for (DistroRead dr : distros) {
         vendors.add(dr.getVendor());
      }
      assertTrue("It must contains vendor, " + Constants.DEFAULT_VENDOR + ".",
            vendors.contains(Constants.DEFAULT_VENDOR));
      assertTrue("It must contains vendor, " + Constants.GPHD_VENDOR + ".",
            vendors.contains(Constants.GPHD_VENDOR));
      assertTrue("It must contains vendor, " + Constants.CDH_VENDOR + ".",
            vendors.contains(Constants.CDH_VENDOR));
      assertTrue("It must contains vendor, " + Constants.HDP_VENDOR + ".",
            vendors.contains(Constants.HDP_VENDOR));
   }

}
