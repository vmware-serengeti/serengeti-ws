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
package com.vmware.bdd.plugin.clouderamgr.service;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.vmware.bdd.plugin.clouderamgr.service.ClouderaManagerImpl;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.plugin.clouderamgr.utils.SerialUtils;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 9:53 AM
 */
public class TestClouderaManagerImpl {
   private static ClouderaManagerImpl provider;

   @BeforeClass(groups = { "TestClouderaManagerImpl" })
   public static void setup() {
      try {
         String privateKey = SerialUtils.dataFromFile("/tmp/privatekey");
         provider = new ClouderaManagerImpl("10.141.72.255", 7180, "admin", "admin", privateKey);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @AfterClass(groups = { "TestClouderaManagerImpl" })
   public static void tearDown() {

   }

   @BeforeMethod(groups = { "TestClouderaManagerImpl" })
   public void beforeMethod() {

   }

   @AfterMethod(groups = { "TestClouderaManagerImpl" })
   public void afterMethod() {

   }

   //@Test(groups = { "TestClouderaManagerImpl" })
   public void testInitializeCluster() throws Exception {
      String content = SerialUtils.dataFromFile("/tmp/serengeti.log");
      ClusterBlueprint blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);
      ClusterReportQueue queue = new ClusterReportQueue();
      provider.createCluster(blueprint, queue);
   }

}
