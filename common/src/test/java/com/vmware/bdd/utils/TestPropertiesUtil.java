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
package com.vmware.bdd.utils;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestPropertiesUtil {

   private File file;
   private static final String TestFile = "test.properties";
   PropertiesUtil propertiesUtil;

   @BeforeClass
   public void init() {
      file = new File(TestFile);
      try {
         if (!file.exists()) {
            file.createNewFile();
         }
      } catch (IOException e) {
      }
      propertiesUtil = new PropertiesUtil(file);
   }

   @AfterClass
   public void deleteFile() {
      if (file.exists()) {
         file.delete();
      }
   }

   @Test(groups = { "TestPropertiesUtil"}, dependsOnMethods = {"testSaveLastKey"})
   public void testGetPropertie() {
      assertEquals(propertiesUtil.getProperty("name"), "name1");
      assertEquals(propertiesUtil.getProperty("value"), "value1");
   }

   @Test(groups = { "TestPropertiesUtil"})
   public void testsetProperties() {
      Properties properties = new Properties();
      properties.put("name", "name1");
      properties.put("value", "value1");
      propertiesUtil.setProperties(properties);
      assertEquals(propertiesUtil.getProperty("name"), properties.get("name"));
      assertEquals(propertiesUtil.getProperty("value"), properties.get("value"));
   }

   @Test(groups = { "TestPropertiesUtil" }, dependsOnMethods = { "testsetProperties" })
   public void testSaveLastKey() {
      StringBuffer sb = new StringBuffer();
      Reader rd = null;
      try {
         propertiesUtil.saveLastKey();
         BufferedInputStream bin =
               new BufferedInputStream(new FileInputStream(file));
         rd = new InputStreamReader(bin, "UTF-8");
         int c = 0;
         while ((c = rd.read()) != -1) {
            sb.append((char) c);
         }
      } catch (IOException e) {
      } finally {
         try {
            if (rd != null) {
               rd.close();
            }
         } catch (IOException e) {
         }
      }
      assertTrue(sb.toString().contains("name=name1"));
      assertTrue(sb.toString().contains("value=value1"));
   }
}
