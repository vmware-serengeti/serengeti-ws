/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestVersion {

   @Test
   public void testCompare() {

      Assert.assertTrue(Version.compare("5.0.2-1.cdh5.0.2.p0.13", "5.0.1") > 0);
      Assert.assertTrue(Version.compare("6", "5") > 0);
      Assert.assertTrue(Version.compare("6", "5.9") > 0);
      Assert.assertTrue(Version.compare("5.1", "5") > 0);
      Assert.assertTrue(Version.compare("5.1", "5.0") > 0);
      Assert.assertTrue(Version.compare("5.1", "5.0.9") > 0);
      Assert.assertTrue(Version.compare("5.1.1", "5.1.0") > 0);

      Assert.assertTrue(Version.compare("5", "5") == 0);
      Assert.assertTrue(Version.compare("5.0", "5") == 0);
      Assert.assertTrue(Version.compare("5", "5.0") == 0);
      Assert.assertTrue(Version.compare("5.0.0", "5") == 0);
      Assert.assertTrue(Version.compare("5.0.0", "5.0") == 0);
      Assert.assertTrue(Version.compare("5.0.0", "5.0.0") == 0);

      Assert.assertTrue(Version.compare("5", "6") < 0);
      Assert.assertTrue(Version.compare("5", "5.1") < 0);
      Assert.assertTrue(Version.compare("5.0", "5.1") < 0);
      Assert.assertTrue(Version.compare("5.1", "5.1.1") < 0);
      Assert.assertTrue(Version.compare("5.1.0", "5.1.1") < 0);
      Assert.assertTrue(Version.compare("5.1.0", "5.2") < 0);
      Assert.assertTrue(Version.compare("5.1.0", "5.2") < 0);

   }
}
