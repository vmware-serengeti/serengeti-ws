/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;

/**
 * <code>Configuration</code> is a util class for accessing configurations of
 * serengeti. <br>
 * 
 * @since 0.1.0
 * @version 0.1.0
 */
public class Configuration {
   private static Logger logger = Logger.getLogger(Configuration.class);
   private static org.apache.commons.configuration.Configuration _config =
         init();

   private static org.apache.commons.configuration.Configuration init() {
      org.apache.commons.configuration.Configuration config = null;
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      String configFileName = null;
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf").append(File.separator).append("serengeti.properties");
         configFileName = builder.toString();
      } else {
         configFileName = "serengeti.properties";
      }
      try {
         logger.info("Reading properties file serengeti.properties");
         config = new PropertiesConfiguration(configFileName);
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Failed to load serengeti configuration file: " + configFileName;
         logger.fatal(message, ex);
         throw BddException.APP_INIT_ERROR(ex, message);
      }
      logConfig(config);
      return config;
   }

   private static void logConfig(
         org.apache.commons.configuration.Configuration config) {
      Iterator<?> keys = config.getKeys();
      while (keys.hasNext()) {
         String key = (String) keys.next();
         logger.debug("Config '" + key + "=" + config.getProperty(key) + "'");
      }
   }

   /**
    * 
    * Gets a property of type int. <br>
    * 
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static int getInt(String key) {
      return _config.getInt(key);
   }

   /**
    * 
    * Gets a property of type int. <br>
    * 
    * @param defaultValue
    *           The default value.
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static int getInt(String key, int defaultValue) {
      return _config.getInt(key, defaultValue);
   }

   /**
    * 
    * Gets a property of type bool. <br>
    * 
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static Boolean getBoolean(String key) {
      return _config.getBoolean(key);
   }

   /**
    * 
    * Gets a property of type bool. <br>
    * 
    * @param defaultValue
    *           The default value.
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static Boolean getBoolean(String key, Boolean defaultValue) {
      return _config.getBoolean(key, defaultValue);
   }

   /**
    * 
    * Gets a property of type string. <br>
    * 
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static String getString(String key) {
      return _config.getString(key);
   }

   /**
    * 
    * Gets a property of type string. <br>
    * 
    * @param defaultValue
    *           The default value.
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static String getString(String key, String defaultValue) {
      return _config.getString(key, defaultValue);
   }

   /**
    * Get all of values in a property as a string type.
    * @param key
    *           The key of property.
    * @param defautValue
    *           The default value.
    * @return The property value.
    */
   public static String getStrings(String key, String defautValue) {
      String[] values = _config.getStringArray(key);
      if (values != null && values.length > 0) {
         StringBuffer buffer = new StringBuffer();
         for (String value : values) {
            buffer.append(value.trim()).append(",");
         }
         buffer.delete(buffer.length() - 1, buffer.length());
         return buffer.toString();
      } else {
         return defautValue;
      }
   }

   /**
    * Gets a whitespace trimmed non-empty string property.
    * 
    * @param key
    *           The key of property.
    * @return The property value or null.
    */
   public static String getNonEmptyString(String key) {
      String s = getString(key, null);
      if (s == null) {
         return null;
      }
      String s1 = s.trim();
      if (s1.isEmpty()) {
         return null;
      } else {
         return s1;
      }
   }

   /**
    * 
    * Gets a property of type double. <br>
    * 
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static Double getDouble(String key) {
      return _config.getDouble(key);
   }

   /**
    * 
    * Gets a property of type double. <br>
    * 
    * @param defaultValue
    *           The default value.
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static Double getDouble(String key, Double defaultValue) {
      return _config.getDouble(key, defaultValue);
   }

   /**
    * 
    * Gets a property of type Long. <br>
    * 
    * @param key
    *           The key of property.
    * @return The property value.
    */
   public static long getLong(String key) {
      return _config.getLong(key);
   }

   /**
    * 
    * Gets a property of type Long. <br>
    * 
    * @param key
    *           The key of property.
    * @param defaultValue
    *           The default value.
    * @return The property value.
    */
   public static long getLong(String key, long defaultValue) {
      return _config.getLong(key, defaultValue);
   }

   /**
    * Test if a key exists
    * 
    * @param key
    *           .
    * @return true if key exists.
    */
   public static boolean containsKey(String key) {
      return _config.containsKey(key);
   }
}
