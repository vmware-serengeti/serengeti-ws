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
package com.vmware.bdd.utils;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;

public class TestConfigSave {

   @Test
   public void testSaveUUID() throws Exception {
      ConfigInfo.setSerengetiUUID("testvAppName");
      ConfigInfo.setInitUUID(false);
      ConfigInfo.save();
      PropertiesConfiguration config = new PropertiesConfiguration(Configuration.getConfigFilePath());
      config.load();
      Assert.assertEquals(config.getString("serengeti.uuid"), "testvAppName", "should get correct uuid");
      Assert.assertEquals(config.getBoolean("serengeti.initialize.uuid"), false, "should not init uuid again");
   }

   @AfterClass
   public static void tearDown() {
      ConfigInfo.setSerengetiUUID("xxx-uuid");
      ConfigInfo.setInitUUID(true);
      ConfigInfo.save();
   }
}
