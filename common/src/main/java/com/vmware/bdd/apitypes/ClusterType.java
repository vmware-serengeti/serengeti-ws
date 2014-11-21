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

/**
 * This class is the common enum of cluster command.
 */
public enum ClusterType {
   DEFAULT("DEFAULT"),

   // hdfs cluster
   HDFS("HDFS"),

   // hdfs + mapreduce cluster
   HDFS_MAPRED("Hadoop"),

   // hdfs + hbase cluster
   HDFS_HBASE("HBase");
   
   private String description;
   ClusterType(String description) {
      assert description != null && !description.isEmpty();
      this.description = description;
   }
   
   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public static ClusterType getByDescription(String description) {
      for (ClusterType type : ClusterType.values()) {
         if (type.getDescription().compareToIgnoreCase(description) == 0) {
            return type;
         }
      }

      return null;
   }
}