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
package com.vmware.bdd.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.spectypes.HadoopRole;

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

   public static String notNull(final String str, final String desStr) {
      return str == null ? desStr : str;
   }

   public static boolean validateName(final String input) {
      return match(input, Constants.NAME_PATTERN);
   }

   public static boolean validatePortGroupName(final String input) {
      return match(input, Constants.PORT_GROUP_NAME_PATTERN);
   }

   public static boolean validateClusterName(final String input) {
      return match(input, Constants.CLUSTER_NAME_PATTERN);
   }

   public static boolean validateNodeGroupName(final String input) {
      return match(input, Constants.NODE_GROUP_NAME_PATTERN);
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

   public static boolean matchDatastorePattern(Set<String> patterns, Set<String> datastores) {
      for (String pattern : patterns) {
         // the datastore pattern is defined as wildcard
         pattern = getDatastoreJavaPattern(pattern);
         for (String datastore : datastores) {
            try {
               if (datastore.matches(pattern)) {
                  return true;
               }
            } catch (Exception e) {
               logger.error(e.getMessage());
               continue;
            }
         }
      }
      return false;
   }

   public static String escapePattern(String pattern) {
      return pattern.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
   }

   public static String getDatastoreJavaPattern(String pattern) {
      return escapePattern(pattern).replace("?", ".").replace("*", ".*");
   }

   public static String getUUID() {
      UUID uuid = UUID.randomUUID();
      return uuid.toString();
   }

   public static long makeVmMemoryDivisibleBy4(long memory) {
      if ((memory % 4) == 0) {
         return memory;
      } else {
         long temp = memory / 4;
         return temp * 4;
      }
   }

   public static boolean isComputeOnly(List<String> roles, String distroVendor) {
      if (distroVendor != null && distroVendor.equalsIgnoreCase(Constants.MAPR_VENDOR)) {
         if (roles != null && roles.contains(HadoopRole.MAPR_TASKTRACKER_ROLE.toString()) &&
               !roles.contains(HadoopRole.MAPR_NFS_ROLE.toString())){
            return true;
         }
      } else {
         if (roles != null && roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())
               && (roles.size() == 1 || (roles.size() == 2 && roles.contains(
               HadoopRole.TEMPFS_CLIENT_ROLE.toString())))) {
            return true;
         }
      }
      return false;
   }

   public static long getVmIndex(String vmName) throws BddException {
      String[] split = vmName.split("-");
      if (split.length < 3) {
         throw ClusteringServiceException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
      try {
         return Long.valueOf(split[2]);
      } catch (Exception e) {
         logger.error("vm name " + vmName
               + " violate serengeti vm name definition.");
         throw ClusteringServiceException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
   }

}
