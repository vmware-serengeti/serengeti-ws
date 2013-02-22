/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import java.util.List;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;

/**
 * Datastore add parameters
 */

public class DatastoreAdd {

   private String name;
   private List<String> spec;
   private DatastoreType type = DatastoreType.SHARED;

   public List<String> getSpec() {
      return spec;
   }

   public void setSpec(List<String> spec) {
      this.spec = spec;
   }

   public DatastoreType getType() {
      return type;
   }

   public void setType(DatastoreType type) {
      this.type = type;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String toString() {
      return (new Gson()).toJson(this);
   }
}
