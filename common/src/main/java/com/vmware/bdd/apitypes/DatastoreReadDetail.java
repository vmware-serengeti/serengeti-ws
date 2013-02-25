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
/**
 * <p>
 * This class is the detail of the DatastoreRead .
 * </p>
 * <p>
 * Create by DuLizhao on 07/05/2012.
 * </p>
 */
package com.vmware.bdd.apitypes;

public class DatastoreReadDetail {
   //the datastore name in VC
   private String vcDatastoreName;
   //the host information
   private String host;
   //total storage size on the host
   private double totalStorageSizeGB;
   //free space on the host
   private double freeSpaceGB;

   public String getVcDatastoreName() {
      return vcDatastoreName;
   }

   public void setVcDatastoreName(String vcDatastoreName) {
      this.vcDatastoreName = vcDatastoreName;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public double getTotalStorageSizeGB() {
      return totalStorageSizeGB;
   }

   public void setTotalStorageSizeGB(double totalStorageSizeGB) {
      this.totalStorageSizeGB = totalStorageSizeGB;
   }

   public double getFreeSpaceGB() {
      return freeSpaceGB;
   }

   public void setFreeSpaceGB(double freeSpaceGB) {
      this.freeSpaceGB = freeSpaceGB;
   }

   @Override
   public String toString() {
      return new StringBuffer().append("[DatastoreReadDetail] ")
            .append("host:").append(this.host).append(",totalStorageSize:")
            .append(this.getTotalStorageSizeGB()).append(",freeSpace:")
            .append(this.freeSpaceGB).toString();
   }


}
