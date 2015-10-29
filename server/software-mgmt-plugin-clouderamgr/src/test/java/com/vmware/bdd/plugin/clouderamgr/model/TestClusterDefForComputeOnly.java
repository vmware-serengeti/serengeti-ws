/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.model;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.utils.SerialUtils;
import com.vmware.bdd.utils.CommonUtil;

public class TestClusterDefForComputeOnly {

   private static ClusterBlueprint blueprint = null;

   @BeforeClass(groups = { "TestClusterDefForComputeOnly" }, dependsOnGroups = {"TestAvailableServiceRoleContainer"})
   public static void setup() throws IOException {

      String content = CommonUtil.readJsonFile("compute_only_blueprint.json");

      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);
   }

   @Test(groups = { "TestClusterDefForComputeOnly" })
   public void testBluePrintToCmCluster() throws IOException {
      blueprint.getHadoopStack().setDistro("CDH-5.2.0");

      CmClusterDef clusterDef = new CmClusterDef(blueprint);
      Assert.assertTrue(clusterDef.getVersion().equals("CDH5"));
      Assert.assertTrue(clusterDef.getFullVersion().equals("5.2.0"));

      Assert.assertTrue(clusterDef.allServiceNames().size() == 2);
      Assert.assertTrue(clusterDef.allServiceNames().contains("cluster01_ISILON"));
      Assert.assertTrue(clusterDef.allServiceTypes().size() == 2);
      Assert.assertTrue(clusterDef.allServiceTypes().contains("ISILON"));

      for (CmServiceDef serviceDef : clusterDef.getServices()) {
         if (serviceDef.getType().getDisplayName().equals("YARN")) {
            Assert.assertNotNull(serviceDef.getConfiguration().get("hdfs_service"));
            Assert.assertNotNull(serviceDef.getConfiguration().get("admin_application_list_settings"));
         }
      }
   }

   @Test(groups = { "TestClusterDefForComputeOnly" })
   public void testIsComputeOnly() throws IOException {
      CmClusterDef clusterDef = new CmClusterDef(blueprint);
      Assert.assertTrue(clusterDef.isComputeOnly());
   }
}
