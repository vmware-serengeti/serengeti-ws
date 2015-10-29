/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.aurora.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * This class provides utility methods to generate localized resources which is
 * visible to the end user (e.g. email contents).
 * 
 * Currently, this class only support localized texts, please add more functions
 * if needed.
 */
public class ResourceManager {
   private static Logger logger = Logger.getLogger(ResourceManager.class);
   private static final ConcurrentMap<String, ResourceBundle> bundles =
         new ConcurrentHashMap<String, ResourceBundle>();
   private static final String DEFAULT_ERROR_MESSAGE = "CANNOT LOAD RESOURCE";

   private static ResourceBundle getResourceBundle(String bundleName, Locale locale)
         throws MissingResourceException {
      AuAssert.check(bundleName != null && !bundleName.isEmpty());
      AuAssert.check(locale != null);

      String bundleKey = bundleName + "@" + locale;
      ResourceBundle bundle = bundles.get(bundleKey);

      if (bundle == null) {
         bundle = ResourceBundle.getBundle(bundleName, locale);
         bundle = bundles.putIfAbsent(bundleKey, bundle);
      }

      return bundle;
   }

   public static String getLocalizedText(String bundleName, Locale locale,
         Enum<?> enumValue) {
      AuAssert.check(bundleName != null && !bundleName.isEmpty());
      AuAssert.check(locale != null);
      AuAssert.check(enumValue != null);

      String key =
            enumValue.getClass().getCanonicalName() + "." + enumValue.name() + ".string";
      try {
         return getResourceBundle(bundleName, locale).getString(key);
      } catch (Exception ex) {
         logger.error("Cannot load enum: " + enumValue.name() + " from " + bundleName
               + " with locale " + locale, ex);
         return enumValue.name();
      }
   }

   public static String getLocalizedText(String bundleName, Locale locale, String key,
         Object... args) {
      AuAssert.check(bundleName != null && !bundleName.isEmpty());
      AuAssert.check(locale != null);

      try {
         ResourceBundle bundle = getResourceBundle(bundleName, locale);

         if (args.length == 0) {
            return bundle.getString(key);
         } else {
            MessageFormat formatter = new MessageFormat(bundle.getString(key), locale);
            return formatter.format(args);
         }
      } catch (Exception ex) {
         logger.error("Cannot load text: " + key + " from " + bundleName
               + " with locale " + locale, ex);
         return DEFAULT_ERROR_MESSAGE;
      }
   }
}
