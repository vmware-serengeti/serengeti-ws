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
package com.vmware.bdd.utils;

public class Version {

   public static int compare(String srcVersion, String destVersion) {
      String[] srcVersionArray = srcVersion.split("\\.");
      String[] destVersionArray = destVersion.split("\\.");
      for (int i = 0; i < srcVersionArray.length; i++) {
         if (i >= destVersionArray.length) {
            return compare(destVersionArray, srcVersionArray, 1);
         }
         if (Integer.parseInt(srcVersionArray[i]) > Integer
               .parseInt(destVersionArray[i])) {
            return 1;
         } else if (Integer.parseInt(srcVersionArray[i]) < Integer
               .parseInt(destVersionArray[i])) {
            return -1;
         }
      }
      if (destVersionArray.length > srcVersionArray.length) {
         return compare(srcVersionArray, destVersionArray, -1);
      }
      return 0;
   }

   public static int compare(String[] srcVersionArray, String[] destVersionArray,
         int type) {
      for (int j = srcVersionArray.length; j < destVersionArray.length; j++) {
         if (Integer.parseInt(destVersionArray[j]) > 0) {
            return type;
         }
      }
      return 0;
   }
}
