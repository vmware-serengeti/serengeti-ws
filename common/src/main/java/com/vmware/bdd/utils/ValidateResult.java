/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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

import java.util.List;

public class ValidateResult {

   public enum Type {
      WHITE_LIST_INVALID_NAME,
      WHITE_LIST_INVALID_VALUE,
      NAME_IN_BLACK_LIST,
      VALID
   }

   private Type type = Type.VALID;
   private List<String> names; // configuration name list
   public Type getType() {
      return type;
   }
   public void setType(Type type) {
      this.type = type;
   }
   public List<String> getNames() {
      return names;
   }
   public void setNames(List<String> names) {
      this.names = names;
   }
}
