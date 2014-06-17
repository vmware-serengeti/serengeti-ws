/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.monitor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

public class ClusterReportQueue {
   private static int DEFAULT_CLUSTER_REPORT_MAX_NUMBER = 10;
   // map operation ID to report info, operation ID should be: clusterName + "_" + Operation + "_" + LaunchTime.
   // i.e, cluster01_create_201405011315, do not use clusterName as ID to avoid potential concurrency issues in future.
   private final BlockingQueue<ClusterReport> reports =
         new ArrayBlockingQueue<ClusterReport>(
               DEFAULT_CLUSTER_REPORT_MAX_NUMBER);

   public ClusterReport pollClusterReport() {
      return reports.poll();
   }

   public void addClusterReport(ClusterReport report) throws SoftwareManagementPluginException {
      try {
         reports.put(report);
      } catch (Exception e) {
         // TODO: refine the exception
         throw new SoftwareManagementPluginException(null, e.getLocalizedMessage(), e);
      }
   }

}
