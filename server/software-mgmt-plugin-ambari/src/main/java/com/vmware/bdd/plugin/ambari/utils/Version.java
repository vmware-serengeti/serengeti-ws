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
package com.vmware.bdd.plugin.ambari.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class Version {
   private static final Logger logger = Logger.getLogger(Version.class);

   public static int compare(String srcVersion, String destVersion) {
      logger.info("The source version is " + srcVersion);
      logger.info("The destination version is " + destVersion);

      srcVersion = getArtifactVersion(srcVersion);
      destVersion = getArtifactVersion(destVersion);

      logger.info("The source artifact version is " + srcVersion);
      logger.info("The destination artifact version is " + destVersion);

      DefaultArtifactVersion srcArtifactVersion = new DefaultArtifactVersion(srcVersion);
      DefaultArtifactVersion destArtifactVersion = new DefaultArtifactVersion(destVersion);

      int[] srcVersionArray = {srcArtifactVersion.getMajorVersion(), srcArtifactVersion.getMinorVersion(), srcArtifactVersion.getIncrementalVersion()};
      int[] destVersionArray = {destArtifactVersion.getMajorVersion(), destArtifactVersion.getMinorVersion(), destArtifactVersion.getIncrementalVersion()};

      for (int i = 0; i < srcVersionArray.length; i++) {
         if (i >= destVersionArray.length) {
            return compare(destVersionArray, srcVersionArray, 1);
         }
         if (srcVersionArray[i] > destVersionArray[i]) {
            return 1;
         } else if (srcVersionArray[i] < destVersionArray[i]) {
            return -1;
         }
      }
      if (destVersionArray.length > srcVersionArray.length) {
         return compare(srcVersionArray, destVersionArray, -1);
      }
      return 0;
   }

   public static int compare(int[] srcVersionArray, int[] destVersionArray,
         int type) {
      for (int j = srcVersionArray.length; j < destVersionArray.length; j++) {
         if (destVersionArray[j] > 0) {
            return type;
         }
      }
      return 0;
   }

   private static String getArtifactVersion(String srcVersion) {
      Pattern pattern = Pattern.compile("(\\d+)(.)(\\d+)(.)(\\d+)");
      Matcher matcher = pattern.matcher(srcVersion);
      while(matcher.find())
      {
         return matcher.group(0);
      }
      return srcVersion;
   }
}
