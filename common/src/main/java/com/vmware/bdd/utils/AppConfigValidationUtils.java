/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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

import java.util.Map;

public class AppConfigValidationUtils {

   public static enum ValidationType {
      WHITE_LIST,
      BLACK_LIST
   }

   /**
    * Validate the configration of spec file.
    * 
    * @param type
    *           validate type,blacklist or whitelist
    * @param config
    *           configration of cluster or nodegroup
    * @return
    */
   public static ValidateResult validateConfig(ValidationType type,
         Map<String, Object> config) {
      ValidateResult validateResult = null;
      if (type == ValidationType.WHITE_LIST) {
         validateResult = AppConfigValidationFactory.whiteListHandle(config);
      } else if (type == ValidationType.BLACK_LIST) {
         validateResult = AppConfigValidationFactory.blackListHandle(config);
      }
      return validateResult;
   }
}
