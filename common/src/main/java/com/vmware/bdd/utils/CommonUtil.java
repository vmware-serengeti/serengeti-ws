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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CommonUtil {

   static final Logger logger = Logger.getLogger(CommonUtil.class);

   public static String readJsonFile(final String fileName) {
      StringBuilder jsonBuff = new StringBuilder();
      URL fileURL = CommonUtil.class.getClassLoader().getResource(fileName);
      if (fileURL != null) {
          InputStream in = null;
          try {
              in = new BufferedInputStream(fileURL.openStream());
              Reader rd = new InputStreamReader(in, "UTF-8");
              int c = 0;
              while ((c = rd.read()) != -1) {
                  jsonBuff.append((char) c);
              }
          } catch (IOException e) {
              logger.error(e.getMessage() + "\n Can not find " + fileName + " or IO read error.");
          } finally {
              try {
            	  if (in != null) {
            		  in.close();
            	  }
              } catch (IOException e) {
                  logger.error(e.getMessage() + "\n Can not close " + fileName + ".");
              }
          }
      }
      return jsonBuff.toString();
  }

   public static List<String> inputsConvert(String inputs) {
      List<String> names = new ArrayList<String>();
      for (String s : inputs.split(",")) {
         if (!s.trim().isEmpty()) {
            names.add(s.trim());
         }
      }
      return names;
   }

   public static boolean isBlank(final String str) {
      return str == null || str.trim().isEmpty();
   }

   public static boolean validateName(final String input) {
      return match(input, Constants.NAME_PATTERN);
   }

   public static boolean validateClusterName(final String input) {
      return match(input, Constants.CLUSTER_NAME_PATTERN);
   }

   public static boolean validateVcDataStoreNames(List<String> names) {
      if (names == null || names.isEmpty()) {
         return false;
      }
      for (String name : names) {
         if (!validateVcDataStoreName(name)) {
            return false;
         }
      }
      return true;
   }

   private static boolean validateVcDataStoreName(final String input) {
      return match(input, Constants.VC_DATASTORE_NAME_PATTERN);
   }

   private static boolean match(final String input, final String regex) {
      Pattern pattern = Pattern.compile(regex);
      return pattern.matcher(input).matches();
   }

}
