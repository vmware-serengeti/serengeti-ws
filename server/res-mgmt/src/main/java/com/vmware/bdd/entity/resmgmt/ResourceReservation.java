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
package com.vmware.bdd.entity.resmgmt;

/**
 * Current resource reservation is placeholder. It will be used in the future
 * to reserve resource for cluster create.
 *
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 */
public class ResourceReservation {

   private String clusterName;

   private String datastoreName;

   //GB
   private int size;

   /**
    * @return the clusterName
    */
   public String getClusterName() {
      return clusterName;
   }

   /**
    * @param clusterName the clusterName to set
    */
   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   /**
    * @return the datastoreName
    */
   public String getDatastoreName() {
      return datastoreName;
   }

   /**
    * @param datastoreName the datastoreName to set
    */
   public void setDatastoreName(String datastoreName) {
      this.datastoreName = datastoreName;
   }

   /**
    * @return the size
    */
   public int getSize() {
      return size;
   }

   /**
    * @param size the size to set
    */
   public void setSize(int size) {
      this.size = size;
   }

}
