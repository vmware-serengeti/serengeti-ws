/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiaoliangl on 8/3/15.
 */
class VcResourceNameRegxFilter<T extends VcObject> implements IVcResourceFilter<T> {
   private List<Pattern> regxList = new ArrayList<>();

   public VcResourceNameRegxFilter(String[] nameRegs) {
      AuAssert.check(ArrayUtils.isNotEmpty(nameRegs), "can't build an empty name regx filter.");

      HashSet<String> nameRegSet = new HashSet<>();
      for (String name : nameRegs) {
         nameRegSet.add(name);
      }

      for (String nameReg : nameRegSet) {
         Pattern pattern = Pattern.compile(nameReg);
         regxList.add(pattern);
      }

   }

   @Override
   public boolean isFiltered(T vcObject) {
      boolean isMatched = false;
      for (Pattern regx : regxList) {
         isMatched = regx.matcher(vcObject.getName()).matches();
         if (isMatched) {
            break;
         }
      }

      return !isMatched;
   }

   public String toString() {
      return new ToStringBuilder(this).append("regxList", regxList).toString();
   }
}
