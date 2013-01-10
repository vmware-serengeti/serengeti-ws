/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.utils.Constants;

public class ClusterCreateTest {

   @Test
   public void testSupportedWithHdfs2() {
      ClusterCreate cluster = new ClusterCreate();
      cluster.setVendor(Constants.DEFAULT_VENDOR);
      cluster.setVersion("1.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setVendor(Constants.CLOUDERA_VENDOR);
      cluster.setVersion("1.0.0");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setVersion("3u3");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setVersion("4.0.1");
      assertEquals(false, cluster.supportedWithHdfs2());
      cluster.setVersion("4.1.2");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setVersion("4.1");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setVersion("4.1.0.2");
      assertEquals(true, cluster.supportedWithHdfs2());
      cluster.setVersion("4.1.0.2.3");
      assertEquals(false, cluster.supportedWithHdfs2());
   }

   @Test
   public void testGetDefaultDistroName() {
      ClusterCreate cluster = new ClusterCreate();
      DistroRead dr1 = new DistroRead();
      dr1.setVendor(Constants.CLOUDERA_VENDOR);
      dr1.setName("CDH");
      Assert.assertNull(cluster.getDefaultDistroName(new DistroRead[] { dr1 }));
      DistroRead dr2 = new DistroRead();
      dr2.setVendor(Constants.DEFAULT_VENDOR);
      dr2.setName("apache");
      assertEquals(dr2.getName(), cluster.getDefaultDistroName(new DistroRead[] { dr1, dr2 }));
   }

}
