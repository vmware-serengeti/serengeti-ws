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
package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashSet;

/**
 * Created by xiaoliangl on 8/3/15.
 */
class VcResourceNameFilter<T extends VcObject> implements IVcResourceFilter<T> {
   private HashSet<String> nameSet = new HashSet<>();


   public VcResourceNameFilter(String[] names) {
      AuAssert.check(ArrayUtils.isNotEmpty(names), "can't build an empty name regx filter.");
      for (String name : names) {
         nameSet.add(name);
      }
   }

   @Override
   public boolean isFiltered(T vcObject) {
      return !nameSet.contains(vcObject.getName());
   }

   public String toString() {
      return new ToStringBuilder(this).append("nameSet", nameSet).toString();
   }
}
