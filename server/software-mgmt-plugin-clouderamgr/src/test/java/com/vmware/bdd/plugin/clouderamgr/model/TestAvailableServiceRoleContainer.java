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
package com.vmware.bdd.plugin.clouderamgr.model;

import com.google.gson.Gson;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import junit.framework.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 7:48 PM
 */
public class TestAvailableServiceRoleContainer {

   @Test(groups = {"TestAvailableServiceRoleContainer"})
   public void testLoad() throws IOException {
      AvailableServiceRoleContainer.loadAll();
      System.out.println(AvailableServiceRoleContainer.dump());
      System.out.println(AvailableServiceRoleContainer.load("HDFS").getAvailableConfigurations().keySet());
   }

   @Test(groups = {"TestAvailableServiceRoleContainer"})
   public void testAllServicesRoles() throws IOException {
      Set<String> allServicesV4 = AvailableServiceRoleContainer.allServices(4);
      Assert.assertTrue(allServicesV4.contains("HDFS"));
      Assert.assertFalse(allServicesV4.contains("YARN")); // CDH4 does not has YARN
      Assert.assertTrue(allServicesV4.contains("ZOOKEEPER"));
      Assert.assertTrue(allServicesV4.contains("HIVE"));
      Set<String> allServicesV5 = AvailableServiceRoleContainer.allServices(5);
      Assert.assertTrue(allServicesV5.contains("YARN")); // CDH5 has YARN
      Set<String> allRoles = AvailableServiceRoleContainer.allRoles(-1);
      Assert.assertTrue(allRoles.contains("HDFS_DATANODE"));
      Assert.assertTrue(allRoles.contains("YARN_NODE_MANAGER"));
   }

   @Test(groups = {"TestAvailableServiceRoleContainer"})
   public void testConfigs() throws IOException {
      String configs = AvailableServiceRoleContainer.getSupportedConfigs(5);
      Map<String, Object> configMap = (new Gson()).fromJson(configs, Map.class);
      Assert.assertTrue(configMap.containsKey("HDFS"));
      Assert.assertTrue(configMap.containsKey("HDFS_DATANODE"));
      List<String> hdfsConfig = (List<String>) configMap.get("HDFS");
      Assert.assertTrue(hdfsConfig.contains("hdfs_missing_blocks_thresholds"));
      System.out.println(configs);
   }
}