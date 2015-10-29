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
package com.vmware.bdd.plugin.ambari.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vmware.bdd.plugin.ambari.utils.MockAmUtils;

import mockit.Mockit;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.CommonUtil;

public class TestAmClusterValidator {

   private static ClusterBlueprint blueprint = null;
   private static AmClusterValidator validator;

   @BeforeClass(groups = { "TestClusterDef" })
   public static void setup() throws IOException {
      Mockit.setUpMock(MockAmUtils.class);

      ApiRootResource apiRootResource = Mockito.mock(ApiRootResource.class);
      RootResourceV1 rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);

      AmbariManagerClientbuilder clientbuilder = Mockito.mock(AmbariManagerClientbuilder.class);
      Mockito.when(clientbuilder.build()).thenReturn(apiRootResource);

      String content = CommonUtil.readJsonFile("simple_blueprint.json");

      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);

      validator = new AmClusterValidator();
      validator.setApiManager(new ApiManager(clientbuilder));
   }

   @Test(groups = { "TestAmClusterValidator" },expectedExceptions = ValidationException.class)
   public void testSuccess() {
      try {
         Assert.assertTrue(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
         throw e;
      }
   }

   @Test(groups = { "TestAmClusterValidator" },expectedExceptions = ValidationException.class)
   public void testUnrecogConfigTypes() {
      try {
         blueprint.getConfiguration().put("hdfs-site.xml",
               new HashMap<String, String>());
         Assert.assertTrue(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
         throw e;
      }
   }

   @Test(groups = { "TestAmClusterValidator" },expectedExceptions = ValidationException.class)
   public void testRecogConfigTypes() {
      try {
         Map<String, String> configItem = new HashMap<String, String>();
         configItem.put("dfs.blocksize", "value");
         blueprint.getConfiguration().put("hdfs-site", configItem);

         Assert.assertTrue(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
         throw e;
      }
   }

   @Test(groups = { "TestAmClusterValidator" },expectedExceptions = ValidationException.class)
   public void testBadConfigItems() {
      try {
         Map<String, String> configItem = new HashMap<String, String>();
         configItem.put("hbase.master.keytab.file", "value");
         blueprint.getConfiguration().put("hdfs-site", configItem);

         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
         throw e;
      }
   }

   @Test(groups = { "TestAmClusterValidator" },expectedExceptions = ValidationException.class)
   public void testMissedRoles() {
      try {
         blueprint.getNodeGroups().get(1).getRoles().remove("HDFS_DATANODE");
         blueprint.getNodeGroups().get(0).getRoles()
         .remove("YARN_RESOURCE_MANAGER");
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
         throw e;
      }
   }

}
