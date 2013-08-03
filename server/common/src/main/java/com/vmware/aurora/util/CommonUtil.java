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
package com.vmware.aurora.util;

import java.io.File;
import java.util.List;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;


/**
 * This class provides static utility methods for common purpose.
 */
public final class CommonUtil {

   /**
    * Check the equality of 2 objects, just like "static Object.equals()" in C#.
    */
   public static boolean testEquals(Object obj1, Object obj2) {
      // first, check if they have same reference
      if (obj1 == obj2) {
         return true;
      }
      // second, check for the null case
      if (obj1 == null) {
         return (obj2 == null);
      }
      // last, check with the overridable "equals" method
      return obj1.equals(obj2);
   }

   public static String combinePaths(String... paths) {
      if (paths == null || paths.length == 0) {
         throw new IllegalArgumentException();
      }
      String path = paths[0];
      for (int i = 1; i < paths.length; i++) {
         path = new File(path, paths[i]).getAbsolutePath();
      }
      return path;
   }

   /**
    * If given condition result is false, adds given message to the list, also returns the condition result.
    * @param cond condition result to check
    * @param condFailMsgList list to add into
    * @param condFailMsg message string to add to the list if condition result is false
    * @return same as given input condition result
    */
   public static boolean checkCond(boolean cond, List<String> condFailMsgList, String condFailMsg) {
      if (!cond && condFailMsgList != null) {
         AuAssert.check(condFailMsg != null);
         condFailMsgList.add(condFailMsg);
      }
      return cond;
   }

   public static PropertiesConfiguration GetPropertiesConfiguration(String filename) throws ConfigurationException {
      PropertiesConfiguration pconf = new PropertiesConfiguration();
      // ','s are value delimiters by default. We don't want ',' delimiters for our error message and properties files
      // as they are not multi-valued. Turn off value delimiters
      // We need to do this before loading the properties
      pconf.setListDelimiter('\0');
      pconf.load(filename);
      return pconf;
   }

   /**
    * Calculate the swap disk size.
    * 0 GB - 1 GB       size of RAM + 0.5G
    * 1 GB - 2 GB       1.5 times the size of RAM
    * 2 GB - 8 GB       Equal to the size of RAM. And if it is <3G, then 3G (that means RAM [2G,3G] all maps to 3G swap).
    * > 8GB            0.75 times the size of RAM. And if it is <8G, then 8G (that means RAM[8G, 10.67G] all maps to 8G swap).
    * @param memSize - the memory size.
    * @return the swap disk size.
    */
   public static int calculateSwapDiskSize(int memSize) {
      if (memSize <= 1024) {
         return memSize + 512;
      } else if (memSize <= 2048) {
         return (int) (1.5 * memSize);
      } else if (memSize <= 3072) {
         return 3072;
      } else if (memSize <= 16384) {
         return memSize;
      } else {
         return 16384;
      }
   }
}
