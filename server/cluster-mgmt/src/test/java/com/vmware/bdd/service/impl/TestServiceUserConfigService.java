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
package com.vmware.bdd.service.impl;

import com.thoughtworks.xstream.alias.ClassMapper;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class TestServiceUserConfigService {
   private ServiceUserConfigService userConfigService;
   private IClusterEntityManager clusterEntityMgr = new ClusterEntityManager();

   @MockClass(realClass = ClusterEntityManager.class)
   private static class MockedClusterEntityManager {
      @Mock
      public ClusterEntity findByName(String clusterName) {
         ClusterEntity entity = new ClusterEntity(clusterName);
         String hadoopConfig = "{\"service_user\": {\n" +
               "    \"HDFS\": {\n" +
               "        \"user_name\": \"hdfs_user_qjin\",\n" +
               "        \"user_group\": \"hadoop_group\",\n" +
               "        \"user_type\": \"LDAP\"\n" +
               "    },\n" +
               "    \"YARN\": {\n" +
               "        \"user_name\": \"yarn_user_qjin\",\n" +
               "        \"user_group\": \"hadoop_group\",\n" +
               "        \"user_type\": \"LDAP\"\n" +
               "    }\n" +
               "   }\n" +
               "}";
         entity.setHadoopConfig(hadoopConfig);
         return entity;
      };

   }
   @BeforeMethod
   public void setUp() throws Exception {
      Mockit.setUpMock(MockedClusterEntityManager.class);
      userConfigService = new ServiceUserConfigService();
      for (Field field: userConfigService.getClass().getFields()) {
         field.setAccessible(true);
      }
      //userConfigService.getClass().getDeclaredField("clusterEntityManager").set(userConfigService, clusterEntityMgr);
   }

   @AfterMethod
   public void tearDown() throws Exception {
      userConfigService = null;
   }

   @Test (expectedExceptions = NullPointerException.class)
   public void testGetServiceUserConfigs() throws Exception {
      MockedClusterEntityManager manager = new MockedClusterEntityManager();
      System.out.println("hadoop config is: " + manager.findByName("cluster").getHadoopConfig());
      userConfigService.getServiceUserConfigs("cluster");
   }

   @Test
   public void testGetServiceUserGroups() throws Exception {
      Map<String, Map<String, String>> serviceUserConfigs = new HashMap<>();
      Map<String, String> config = new HashMap<>();
      config.put(UserMgmtConstants.SERVICE_USER_GROUP, "hadoop_group");
      serviceUserConfigs.put("HDFS", config);
      assertTrue(userConfigService.getServiceUserGroups(serviceUserConfigs).contains("hadoop_group"));
   }
}