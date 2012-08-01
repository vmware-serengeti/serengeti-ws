/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Storage get output
 */
public class StorageRead {
   @Expose
   private String type;
   
   @Expose
   @SerializedName("size")
   private int sizeGB;
   private List<String> dsNames;
   @Expose
   @SerializedName("name_pattern")
   private List<String> namePattern;

   public List<String> getDsNames() {
      return dsNames;
   }

   public void setDsNames(List<String> dsNames) {
      this.dsNames = dsNames;
   }

   public List<String> getNamePattern() {
      return namePattern;
   }

   public void setNamePattern(List<String> namePattern) {
      this.namePattern = namePattern;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public int getSizeGB() {
      return sizeGB;
   }

   public void setSizeGB(int sizeGB) {
      this.sizeGB = sizeGB;
   }

}
