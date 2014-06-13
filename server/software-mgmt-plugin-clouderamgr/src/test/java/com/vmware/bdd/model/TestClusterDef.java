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
package com.vmware.bdd.model;

import com.google.gson.Gson;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.SerialUtils;
import org.codehaus.jackson.JsonParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Author: Xiaoding Bian
 * Date: 6/13/14
 * Time: 3:17 PM
 */
public class TestClusterDef {

   private static ClusterBlueprint blueprint = null;

   @BeforeClass
   public static void setup() throws IOException {

      String content = CommonUtil.readJsonFile("simple_blueprint.json");

      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);
   }

   @Test
   public void testBluePrintToCmCluster() throws IOException {

      CmClusterDef clusterDef = new CmClusterDef(blueprint);

      Gson gson = new Gson();

      System.out.println(gson.toJson(clusterDef));

      System.out.println(gson.toJson(clusterDef.ipToNode()));

      System.out.println(gson.toJson(clusterDef.ipToRoles()));
   }

}
