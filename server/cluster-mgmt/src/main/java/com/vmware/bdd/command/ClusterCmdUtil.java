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
package com.vmware.bdd.command;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.vmware.bdd.utils.AuAssert;

public class ClusterCmdUtil {
   private static final Level logLevel = Logger
         .getRootLogger().getLevel();


   public static String getLogLevel() {
      if (logLevel.isGreaterOrEqual(Level.ERROR)) {
         return "";
      } else if (logLevel.isGreaterOrEqual(Level.INFO)) {
         return "-V";
      } else { 
         return "-VV";
      }
   }
   
   public static String getFullNodeName(String cluster, String group,
         String node) {
      AuAssert.check(cluster != null && !cluster.isEmpty());
      AuAssert.check(group == null || !group.isEmpty());
      AuAssert.check(node == null || !node.isEmpty());
      AuAssert.check(!(node != null && group == null));

      if (node != null) {
         return node; // node is already the full name
      }

      if (group == null) {
         return cluster;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append(cluster).append("-").append(group);
         return sb.toString();
      }
   }

   public static int getIndexFromNodeName(String node) {
      AuAssert.check(node != null && !node.isEmpty());

      String[] ary = node.split("-");
      AuAssert.check(ary.length == 3);

      return Integer.parseInt(ary[2]);
   }
}
