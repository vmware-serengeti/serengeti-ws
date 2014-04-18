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
package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.utils.VcResourceUtils;

public class SyncHostsUtils {
   private static final Logger logger = Logger.getLogger(SyncHostsUtils.class);

   public static void SyncHosts(ClusterCreate clusterSpec, Set<String> hostnames) {
      int maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC;
      if (clusterSpec.checkHBase())
         maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC_HBASE;
      List<String> outOfSyncHosts = new ArrayList<String>();
      for (String hostname : hostnames) {
         int hostTimeDiffInSec =
               VcResourceUtils.getHostTimeDiffInSec(hostname);
         if (Math.abs(hostTimeDiffInSec) > maxTimeDiffInSec) {
            logger.info("Host " + hostname + " has a time difference of "
                  + hostTimeDiffInSec
                  + " seconds and is dropped from placement.");
            outOfSyncHosts.add(hostname);
         }
      }
      if (!outOfSyncHosts.isEmpty()) {
         String managementServerHost = VcResourceUtils.getManagementServerHost();
         logger.error("Time on host " + outOfSyncHosts
               + "is out of sync which will lead to failure, "
               + "synchronize the time on these hosts with "
               + "Serengeti management server and try again.");
         throw TaskException.HOST_TIME_OUT_OF_SYNC(outOfSyncHosts, managementServerHost);
      }
   }
}
