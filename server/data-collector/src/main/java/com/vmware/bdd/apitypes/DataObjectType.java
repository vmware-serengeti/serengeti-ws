/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.apitypes;

import com.vmware.bdd.util.collection.CollectionConstants;

public enum DataObjectType {

   FOOTPRINT(CollectionConstants.BDE_FOOTPRINT),
   ENVIRONMENTAL_INFORMATION(CollectionConstants.BDE_ENVIRONMENTAL_INFORMATION),
   COMMONREPORTS(CollectionConstants.BDE_PRODUCT_INSTANCE),
   OPERATION(CollectionConstants.BDE_OPERATION),
   CLUSTER_SNAPSHOT(CollectionConstants.BDE_CLUSTER_SNAPSHOT);

   private String name;

   DataObjectType(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
