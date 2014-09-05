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
package com.vmware.bdd.utils;

import java.util.List;

/**
 * convert a list to a string separated by a char.
 */
public class ListToStringConverter<T> {
   private final String str;

   public ListToStringConverter(List<T> list, char separator) {
      int size = list.size();
      int count = 0;
      StringBuilder sb = new StringBuilder();
      for(T t : list) {
         count ++;
         sb.append(t.toString());

         if(count != size) {
            sb.append(separator);
         }
      }
      str = sb.toString();
   }

   public String toString() {
      return str;
   }
}
