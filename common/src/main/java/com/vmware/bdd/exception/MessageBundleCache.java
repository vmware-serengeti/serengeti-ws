/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.exception;

import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created By xiaoliangl on 1/22/15.
 */
public class MessageBundleCache {
   private final static ConcurrentHashMap<String, Messages> CACHE = new ConcurrentHashMap<String, Messages>();

   public static Messages get(String bundleName) {
      Messages bundle = CACHE.get(bundleName);
      if(bundle != null) {
         return bundle;
      }
      Messages messages = new Messages(ResourceBundle.getBundle(bundleName));

      CACHE.putIfAbsent(bundleName, messages);

      return messages;
   }
}
