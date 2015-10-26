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

package com.vmware.aurora.global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.exception.CommonException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.util.StringUtil;

/**
 * <code>Configuration</code> is a util class for accessing configurations of
 * CMS. <br>
 * 
 * @since 0.4.2
 * @version 0.4.2
 * @author Gene Zhang
 */
public class Configuration {
   private static Logger logger = Logger.getLogger(Configuration.class);
   private static String configFileName;
   private static PropertiesConfiguration config = init();
   private static PropertiesConfiguration serengetiCfg;
   private static PropertiesConfiguration vcCfg;
   private static String storedCmsInstanceId;
   private static final String NGC_PROP_FILE = "ngc_registrar.properties";
   private static PropertiesConfiguration ngcCfg;

   /**
    * 
    * @return a memory view of all properties inside serengeti.properties and vc.properties
    */
   private static PropertiesConfiguration init() {
      PropertiesConfiguration config = null;

      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      String ngcConfigFile = NGC_PROP_FILE;
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf")
               .append(File.separator);
         configFileName = builder.toString() + "serengeti.properties";
         ngcConfigFile = builder.toString() + NGC_PROP_FILE;
      } else {
         configFileName = "serengeti.properties";
      }

      try {
         URL url = ConfigurationUtils.locate(null, configFileName);
         logger.info("Reading properties file serengeti.properties from " + url.getPath());
         serengetiCfg = new PropertiesConfiguration();
         serengetiCfg.setEncoding("UTF-8");
         serengetiCfg.setFileName(configFileName);
         serengetiCfg.load();
         config = (PropertiesConfiguration) serengetiCfg.clone();
      } catch (ConfigurationException ex) {
         String message = "Failed to load serengeti.properties file.";
         logger.fatal(message, ex);
         throw AuroraException.APP_INIT_ERROR(ex, message);
      }

      String propertyFilePrefix =
            System.getProperty("PROPERTY_FILE_PREFIX", "vc");
      String propertyFileName = propertyFilePrefix + ".properties";

      try {
         logger.info("Reading properties file " + propertyFileName);
         vcCfg = new PropertiesConfiguration(propertyFileName);
         Iterator<?> keys = vcCfg.getKeys();
         while (keys.hasNext()) {
            String key = (String) keys.next();
            config.setProperty(key, vcCfg.getProperty(key));
         }
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Failed to load vc.properties file.";
         logger.fatal(message, ex);
         throw AuroraException.APP_INIT_ERROR(ex, message);
      }
   // load ngc_registrar.properties
      try {
         logger.info("Reading properties file " + ngcConfigFile);
         ngcCfg = new PropertiesConfiguration(ngcConfigFile);
         ngcCfg.setEncoding("UTF-8");
         Iterator<?> keys = ngcCfg.getKeys();
         while (keys.hasNext()) {
            String key = (String) keys.next();
            config.setProperty(key, ngcCfg.getProperty(key));
         }
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Failed to load file " + NGC_PROP_FILE;
         logger.fatal(message, ex);
         throw AuroraException.APP_INIT_ERROR(ex, message);
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
      return config.getInt(key);
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
      try {
         return config.getInt(key, defaultValue);
      } catch (ConversionException ce) {
         logger.error(key + " in serengeti.properties is not an integer!");
      }

      return defaultValue;
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
      return config.getBoolean(key);
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
      try {
         return config.getBoolean(key, defaultValue);
      } catch (ConversionException ce) {
         logger.error(key + " in serengeti.properties is not a boolean!");
      }

      return defaultValue;
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
      return config.getString(key);
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
      return config.getString(key, defaultValue);
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
      return StringUtil.trimNonEmpty(s);
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
      return config.getDouble(key);
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
      try {
         return config.getDouble(key, defaultValue);
      } catch (ConversionException ce) {
         logger.error(key + " in serengeti.properties is not a double!");
      }

      return defaultValue;
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
      return config.getLong(key);
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
      try {
         return config.getLong(key, defaultValue);
      } catch (ConversionException ce) {
         logger.error(key + " in serengeti.properties is not a long!");
      }

      return defaultValue;
   }

   /**
    * Test if a key exists
    * 
    * @param key
    *           .
    * @return true if key exists.
    */
   public static boolean containsKey(String key) {
      return config.containsKey(key);
   }

   /**
    * Return the CMS instance identifier, global to this instance of the CMS.
    * 
    * @return CMS instance identifier
    */
   public static String getCmsInstanceId() {
      /*
       * We have two sources for the instance ID: the database ("stored instance
       * id") and properties file ("bootstrap instance id"). We prefer the
       * stored instance id, but use the bootstrap one if it's not available
       * (mostly for bootstrapping, also for unit tests that don't have a
       * database).
       *
       * In normal use, during the setup process we read the bootstrap instance
       * id and store it in the database, after which we always use the stored
       * one. (They would be the same unless some disaster happens where the
       * VM gets reprovisioned from backup and the bootstrap one in the properties
       * file changes, which is why we prefer using the one we stored in the db.)
       *
       * As a further complication, for dependency reasons we want to expose the
       * instance ID from this layer, but also for dependency reasons we're unable
       * to read the DB here. So we can't read the stored instance ID ourselves;
       * we require some other layer (CmsConfig) to read it and inject it here.
       * VirtualCenterResourceManagement will copy the bootstrap instance ID to
       * the DB during setup, and also call us, and thereafter Organization will
       * call us during system initialization.
       */
      if (bootstrapMode == Configuration.BootstrapUsage.ALLOWED) {
         // In the window after we notice setup hasn't run yet and before we run
         // setup, we initialize the VC context and it's allowed to use the
         // bootstrap instance ID.
         AuAssert.check(storedCmsInstanceId == null);
         return getBootstrapInstanceId();
      }
      // Otherwise, we require that we're using the stored instance ID.
      AuAssert.check(storedCmsInstanceId != null);
      return storedCmsInstanceId;
   }

   public enum BootstrapUsage {
      DISALLOWED, ALLOWED, FINALIZED;
   }

   private static BootstrapUsage bootstrapMode = BootstrapUsage.DISALLOWED;

   /**
    * Set the CMS instance identifier which will be returned by
    * getCmsInstanceId(). Can be called only once per process during
    * initialization. This variant is used only during system setup
    * (VirtualCenterResourceManagement.initializeSystem); otherwise
    * setCmsInstanceId is preferred.
    */
   public static String approveBootstrapInstanceId(BootstrapUsage usage) {
      switch (usage) {
      case ALLOWED:
         // transition from DISALLOWED to ALLOWED
         AuAssert.check(bootstrapMode == BootstrapUsage.DISALLOWED);
         AuAssert.check(storedCmsInstanceId == null);
         bootstrapMode = usage;
         return null;
      case FINALIZED:
         // transition from ALLOWED to FINALIZED
         AuAssert.check(bootstrapMode == BootstrapUsage.ALLOWED);
         AuAssert.check(storedCmsInstanceId == null);
         setCmsInstanceId(getBootstrapInstanceId());
         bootstrapMode = usage;
         return getCmsInstanceId();
      default:
         // no other transition is legal
         AuAssert.check(false);
         return null;
      }
   }

   /**
    * Set the CMS instance identifier which will be returned by
    * getCmsInstanceId(). Can be called only once per process during
    * initialization.
    * 
    * @param id
    *           Instance ID retrieved from database.
    */
   public static void setCmsInstanceId(String id) {
      /*
       * This can be called only once per process, so the value can't change after
       * it's set. The current caller is CmsConfig.init().
       *
       * We require this be injected from an external source because we can't
       * calculate it here due to dependencies; however (also due to dependencies)
       * we want to provide access to this value from here. So we're happy to
       * store-and-forward here.
       */
      AuAssert.check(storedCmsInstanceId == null);
      storedCmsInstanceId = id;

      // sanity check
      String bootstrapId = getBootstrapInstanceId();
      if (!id.equals(bootstrapId)) {
         logger.warn("Instance ID has changed from " + bootstrapId + " to "
               + id);
      }
   }

   /**
    * Generate the instance ID from the stored moref to the CMS VM.
    * 
    * @return Bootstrap instance ID.
    */
   private static String getBootstrapInstanceId() {
      String cmsVmIdText = Configuration.getString("vim.cms_moref");
      int pos = cmsVmIdText.lastIndexOf('-');
      if (pos == -1) {
         logger.error("Unrecognized CMS VM moref Id: " + cmsVmIdText);
         throw CommonException.INTERNAL();
      }
      Long cmsVmId = Long.parseLong(cmsVmIdText.substring(pos + 1));
      return Integer.toHexString(cmsVmId.hashCode()); // enforce maximum of 8 digits
   }

   /**
    * Gets the fqdn of cms server. If it's not presented, the method tries to
    * retrieve the ip address of first network interface and reports it as the
    * fqdn.
    * 
    * @return The FQDN of CMS.
    */
   public static String getCmsFQDN() {
      String fqdn = Configuration.getString("cms.mgmtnet.fqdn");
      if (fqdn == null || fqdn.isEmpty()) {
         try {
            // try to retrieve the ip addr of eth0
            NetworkInterface net = NetworkInterface.getByName("eth0");
            Enumeration<InetAddress> addresses = net.getInetAddresses();
            while (addresses.hasMoreElements()) {
               InetAddress addr = addresses.nextElement();
               if (addr instanceof Inet4Address) {
                  String ip = addr.toString();
                  fqdn = ip.substring(ip.lastIndexOf("/") + 1);
                  break;
               }
            }
         } catch (Exception e) {
            logger.info("Error in retrieving ip of eth0", e);
            throw CommonException.INTERNAL(e);
         }
      }
      return fqdn;
   }

   /**
    * Get all of values in a property as a String[] type.
    *
    * @param key
    *           The key of property.
    * @param defautValue
    *           The default value.
    * @return The property value.
    */
   public static String[] getStringArray(String key, String[] defautValue) {
      String[] values = config.getStringArray(key);
      if(values != null && values.length == 4)
         return values;
      return defautValue;
   }

   /**
    * Get all of values in a property as a string type.
    * 
    * @param key
    *           The key of property.
    * @param defautValue
    *           The default value.
    * @return The property value.
    */
   public static String getStrings(String key, String defautValue) {
      String[] values = config.getStringArray(key);
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
    * Set the boolean value of a given key
    * 
    * @param key
    * @param value
    */
   public static void setBoolean(String key, Boolean value) {
      config.setProperty(key, value);
      if (vcCfg.containsKey(key)) {
         vcCfg.setProperty(key, value);
      } else {
         serengetiCfg.setProperty(key, value);
      }
   }

   public static void setString(String key, String value) {
      config.setProperty(key, value);
      if (vcCfg.containsKey(key)) {
         vcCfg.setProperty(key, value);
      } else {
         serengetiCfg.setProperty(key, value);
      }
   }

   public static void save() {
      OutputStream out = null;
      try {
         // we only have reqs to save serengeti.properties currently
         out = new FileOutputStream(new File(serengetiCfg.getPath()));
         serengetiCfg.save(out);
      } catch (Exception ex) {
         // error out if the configuration file is not there
         String message =
               "Failed to save serengeti configuration file: " + configFileName;
         logger.fatal(message, ex);
      } finally {
         try {
            if (out != null) {
               out.close();
            }
         } catch (IOException e) {
            logger.error("Failed to close file: " + configFileName
                  + ", after save configuration.");
         }
      }
   }
}