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
package com.vmware.bdd.manager.collection;

import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.collection.impl.FakeCollectionInitializerService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.PropertiesUtil;

public class TestCollectionDriverManager {

   private CollectionDriverManager collectionDriverManager;
   private static CollectionDriver collectionDriver;
   private ICollectionInitializerService collectionInitializerService;
   private CollectOperationManager collectOperationManager;
   private DataContainer dataContainer;
   private static File file;
   private static String FILE_PATH = "/tmp/test.properties";

   @MockClass(realClass = PropertiesUtil.class)
   public static class MockPropertiesUtil {

      PropertiesUtil propertiesUtil = new PropertiesUtil(file);

      @Mock
      public String getProperty(String key) {
         return "true";
      }

      @Mock
      public PropertiesUtil setProperty(String key, String value) {
         Properties prop = new Properties();
         prop.setProperty(key, value);
         propertiesUtil.setProperties(prop);
         return propertiesUtil;
      }
   }

   private static void setUp() {
      file = new File(FILE_PATH);
      if (!file.exists()) {
         try {
            file.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   @BeforeMethod(groups = { "TestCollectionDriverManager" })
   public void setMockup() {
      setUp();
      Mockit.setUpMock(MockPropertiesUtil.class);
      collectionDriver = Mockito.mock(FakeCollectionDriver.class);
      collectionInitializerService =
            Mockito.mock(FakeCollectionInitializerService.class);
      collectOperationManager = mock(CollectOperationManager.class);
      dataContainer = mock(DataContainer.class);
      collectionDriverManager =
            new FakeCollectionDriverManager(
                  "com.vmware.bdd.manager.ph.PhoneHomeCollectionDriver",
                  collectionInitializerService, collectionDriver,
                  collectOperationManager, dataContainer, file);
      Mockito.when(collectionDriver.getCollectionSwitchName()).thenReturn(
            "serengeti.ph.enable");
   }

   @AfterMethod
   public void tearDown() {
      Mockit.tearDownMocks();
      cleanUpData();
   }

   private void cleanUpData() {
      if (file.exists()) {
         file.delete();
      }
   }

   @Test(groups = { "TestCollectionDriverManager" })
   public void testChangeCollectionSwitchStatus() {
      collectionDriverManager.changeCollectionSwitchStatus(true);
      try {
         String propertiesStr = CommonUtil.dataFromFile(FILE_PATH);
         assertTrue(propertiesStr.contains("serengeti.ph.enable=true"));
      } catch (IOException e) {
      }
   }

   @Test(groups = { "TestCollectionDriverManager" }, dependsOnMethods = { "testChangeCollectionSwitchStatus" })
   public void testGetCollectionSwitchStatus() {
      assertTrue(collectionDriverManager.getCollectionSwitchStatus());
   }

}
