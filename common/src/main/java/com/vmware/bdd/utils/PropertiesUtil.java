/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertiesUtil {

   static final Logger logger = Logger.getLogger(PropertiesUtil.class);
   private Properties properties = new Properties();
   private File file;

   public PropertiesUtil(File file) {
      this.file = file;
      try {
         loadProperties(this.file);
      } catch (IOException e) {
         logger.warn("Fail to load file: " + e.getLocalizedMessage());
      }
   }

   private void loadProperties(File file) throws IOException {
      InputStream ips = null;
      try {
         ips = new FileInputStream(file);
         properties.load(ips);
      } finally {
         ips.close();
      }
   }

   public String getProperty(String key) {
      String value = properties.getProperty(key);
      return CommonUtil.notNull(value, "");
   }

   public PropertiesUtil setProperty(String key, String value) {
      properties.setProperty(key, value);
      return this;
   }

   public void saveLastKey() throws IOException {
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(file);
         properties.store(fos, null);
      } finally {
         if (fos != null) {
            fos.close();
         }
      }
   }

   public Properties getProperties() {
      return properties;
   }

   public void setProperties(Properties properties) {
      this.properties = properties;
   }

}