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
package com.vmware.bdd.plugin.ambari.model;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.CommonUtil;

public class TestAmClusterDef {

   private static ClusterBlueprint blueprint = null;

   @BeforeClass(groups = { "TestClusterDef" })
   public static void setup() throws IOException {

      String content = CommonUtil.readJsonFile("simple_blueprint.json");

      blueprint =
            SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);
   }

   @Test(groups = { "TestClusterDef" })
   public void testBluePrintToAmCluster() throws IOException {
      blueprint.getHadoopStack().setDistro("HDP", "2.1");

      AmClusterDef clusterDef = new AmClusterDef(blueprint, null, "1.7");
      Assert.assertTrue(clusterDef.getVersion().equals("2.1"));

      Gson gson = new Gson();
      System.out.println(gson.toJson(clusterDef.toApiBootStrap()));
      System.out.println(gson.toJson(clusterDef.toApiBlueprint()));
      System.out.println(gson.toJson(clusterDef.toApiClusterBlueprint()));

      Assert.assertTrue(clusterDef.isVerbose());
      Assert.assertTrue(clusterDef.getUser() == "serengeti");

      for (AmNodeDef node : clusterDef.getNodes()) {
         Assert.assertTrue(node.getFqdn().isEmpty() == false);
         if (node.getComponents().contains("NAMENODE")) {
            Assert.assertTrue(node.getConfigurations().get(0).containsKey("global"));
         }
      }

   }
}
