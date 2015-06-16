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
package com.vmware.bdd.plugin.ambari.api.model.stack;

public class ComponentName {

   public static boolean isNamenode(String componentName) {
       return "NAMENODE".equals(componentName);
   }

   public static boolean isSecondaryNamenode(String componentName) {
      return "SECONDARY_NAMENODE".equals(componentName);
   }

   public static boolean isJournalnode(String componentName) {
      return "JOURNALNODE".equals(componentName);
   }

   public static boolean isZkfc(String componentName) {
      return "ZKFC".equals(componentName);
   }

}
