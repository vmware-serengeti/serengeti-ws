/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.spectypes;

public enum HadoopRole {

   HADOOP_NAMENODE_ROLE("NAMENODE"),

   HADOOP_SECONDARY_NAMENODE_ROLE("SECONDARY_NAMENODE"),

   HADOOP_JOBTRACKER_ROLE("JOBTRACKER"),

   HADOOP_RESOURCEMANAGER_ROLE("RESOURCEMANAGER"),

   ZOOKEEPER_SERVER_ROLE("ZOOKEEPER_SERVER");

   private String description;

   private HadoopRole(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }
}
