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
package com.vmware.bdd.plugin.clouderamgr.utils;

import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 7/11/14
 * Time: 3:07 PM
 */
public class TestCmUtils {

   @Test
   public void testMajorVersionOfStack() {
      HadoopStack stack = new HadoopStack();
      stack.setDistro("CDH-5.0.1");
      Assert.assertEquals(CmUtils.distroVersionOfHadoopStack(stack), "5.0.1");
      stack.setDistro("CDH");
      Assert.assertEquals(CmUtils.distroVersionOfHadoopStack(stack), "-1");
      stack.setDistro(null);
      Assert.assertEquals(CmUtils.distroVersionOfHadoopStack(stack), "-1");
   }

   @Test
   public void testIsValidRack() {
      Assert.assertTrue(CmUtils.isValidRack("/rack"));
      Assert.assertTrue(CmUtils.isValidRack("/rack-01"));
      Assert.assertTrue(CmUtils.isValidRack("/.rack-01"));
      Assert.assertTrue(CmUtils.isValidRack("/_rack-01"));
      Assert.assertTrue(CmUtils.isValidRack("/rack-01/rack-02"));
      Assert.assertFalse(CmUtils.isValidRack("rack-01/rack-02"));
   }

}
