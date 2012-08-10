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

import java.util.ArrayList;
import java.util.List;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;

/**
 * <p>
 * This class is the DTO of Datastore list command.
 * </p>
 */
public class DatastoreRead {

   //datastore name in Serengeti
   private String name;
   //LOCAL/SHARED
   private DatastoreType type;
   //total space
   private double totalSpaceGB;
   //free space
   private double freeSpaceGB;
   //detail of the DatastoreRead
   private List<DatastoreReadDetail> datastoreReadDetails =
         new ArrayList<DatastoreReadDetail>();


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

   public double getTotalSpaceGB() {
      return totalSpaceGB;
   }

   public void setTotalSpaceGB(double totalSpaceGB) {
      this.totalSpaceGB = totalSpaceGB;
   }

   public double getFreeSpaceGB() {
      return freeSpaceGB;
   }

   public void setFreeSpaceGB(double freeSpaceGB) {
      this.freeSpaceGB = freeSpaceGB;
   }


   public List<DatastoreReadDetail> getDatastoreReadDetails() {
      return datastoreReadDetails;
   }

   public void setDatastoreReadDetails(
         List<DatastoreReadDetail> datastoreReadDetails) {
      this.datastoreReadDetails = datastoreReadDetails;
   }

   @Override
   public String toString() {
      return new StringBuffer().append("[DatastoreRead] ").append("name:")
            .append(this.name).append(",type:").append(this.type)
            .append(",totalSpace:").append(this.totalSpaceGB)
            .append(",freeSpace:").append(this.freeSpaceGB)
            .append(",datastoreReadDetails:").append(this.datastoreReadDetails)
            .toString();
   }


}
