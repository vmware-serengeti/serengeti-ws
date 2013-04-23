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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Storage get output
 */
public class StorageRead {

   public enum DiskSplitPolicy {
      // separate this disk on datastores as much as possible
      EVEN_SPLIT,
      // separate this disk on datastore as less as possible
      AGGREGATE,
      // separate this disk onto at most two datastores, first try with two if possible 
      BI_SECTOR
   }

   public enum DiskScsiControllerType {
      LSI_CONTROLLER, 
      PARA_VIRTUAL_CONTROLLER
   }

   @Expose
   private String type;

   @Expose
   private Priority shares;

   @Expose
   @SerializedName("size")
   private int sizeGB;

   private List<String> dsNames;

   @Expose
   @SerializedName("name_pattern")
   private List<String> namePattern;

   @SerializedName("split_policy")
   private DiskSplitPolicy splitPolicy;
   private DiskScsiControllerType controllerType;
   private String allocType;

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

   public DiskSplitPolicy getSplitPolicy() {
      return splitPolicy;
   }

   public void setSplitPolicy(DiskSplitPolicy splitPolicy) {
      this.splitPolicy = splitPolicy;
   }
   public Priority getShares() {
      return shares;
   }

   public void setShares(Priority shares) {
      this.shares = shares;
   }

   public DiskScsiControllerType getControllerType() {
      return controllerType;
   }

   public void setControllerType(DiskScsiControllerType controllerType) {
      this.controllerType = controllerType;
   }

   public String getAllocType() {
      return allocType;
   }

   public void setAllocType(String allocType) {
      this.allocType = allocType;
   }
}
