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
package com.vmware.bdd.plugin.clouderamgr.utils;

import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;

import java.util.regex.Pattern;

/**
 * Author: Xiaoding Bian
 * Date: 7/11/14
 * Time: 1:42 PM
 */
public class CmUtils {

   public static String distroVersionOfHadoopStack(HadoopStack stack) {
      try {
         String[] versionInfo = stack.getDistro().split("-");
         return (versionInfo[1]);
      } catch (Exception e) {
         return Constants.VERSION_UNBOUNDED;
      }
   }

   public static boolean isValidRack(String rack) {
      Pattern rackPattern = Pattern.compile("(/[a-zA-Z0-9\\.\\-\\_]+)+");
      return rackPattern.matcher(rack).matches();
   }
}
