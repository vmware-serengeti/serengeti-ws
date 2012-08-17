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
package com.vmware.bdd.spectypes;

public enum HadoopRole {
   PIG_ROLE("pig"), 
   HIVE_ROLE("hive"),
   HIVE_SERVER_ROLE("hive_server"), 
   HADOOP_CLIENT_ROLE("hadoop_client"),
   HADOOP_DATANODE("hadoop_datanode"), 
   HADOOP_TASKTRACKER("hadoop_tasktracker"), 
   HADOOP_JOBTRACKER_ROLE("hadoop_jobtracker"), 
   HADOOP_NAMENODE_ROLE("hadoop_namenode");

   private String description;

   private HadoopRole(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }

   public static HadoopRole fromString(String desc) {
      if (desc != null) {
         for (HadoopRole b : HadoopRole.values()) {
            if (desc.equalsIgnoreCase(b.toString())) {
               return b;
            }
         }
      }
      return null;
   }
}
