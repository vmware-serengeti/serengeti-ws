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
package com.vmware.bdd.service.sp;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.exception.ClusterOperationCallbackException;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ShellCommandExecutor;

public class ClusterOperationCallbackSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(ClusterOperationCallbackSP.class);

   private String phase;
   private String clusterName;
   private String clusterOperation;
   private String appMgrName;
   private String vendorName;
   private String distroVersion;
   private int clusterOperationCallbackTimeout;
   private String softwareMgrVersion;

   public ClusterOperationCallbackSP(String phase, String clusterName, String clusterOperation, String appMgrName, String vendorName, String distroVersion, String softwareMgrVersion) {
      this.phase = phase;
      this.clusterName = clusterName;
      this.clusterOperation = clusterOperation.toLowerCase();
      this.appMgrName = appMgrName;
      this.vendorName = vendorName;
      this.distroVersion = distroVersion;
      this.clusterOperationCallbackTimeout = Configuration.getInt(Constants.CLUSTER_OPERATION_CALLBACK_TIMEOUT_SECONDS, Constants.CLUSTER_OPERATION_CALLBACK_TIMEOUT_SECONDS_DEFAULT);
      this.softwareMgrVersion = softwareMgrVersion;
   }

   @Override
   public Void call() throws Exception {
      invoke();
      return null;
   }

   public boolean invoke() throws Exception {

      String clusterOperationCallbackcommand = Constants.CLUSTER_OPERATION_CALLBACK_SCRIPT_FILE_NAME + " " + phase + " " + clusterName + " " + clusterOperation + " " + appMgrName + " " + vendorName + " " + distroVersion + " " + softwareMgrVersion;

      try {
         logger.info("Executing cluster operation hooks: " + clusterOperationCallbackcommand);
         ShellCommandExecutor.execCmd(clusterOperationCallbackcommand, new File(Constants.DEFAULT_SERENGETI_SBIN_DIR), null, clusterOperationCallbackTimeout, Constants.CLUSTER_OPERATION_CALLBACK_DESCRIPTION);
         return true;
      } catch (Exception e) {
         throw ClusterOperationCallbackException.FAIL_TO_EXECUTE_CLUSTER_OPERATION_CALLBACK(clusterOperationCallbackcommand, e.getMessage());
      }
   }
}
