/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.SetVMPasswordSP;
import com.vmware.bdd.utils.AuAssert;

public class SetPasswordService implements ISetPasswordService {

   private static final Logger logger = Logger.getLogger(SetPasswordService.class);

   public ArrayList<String> setPasswordForNodes(String clusterName, ArrayList<String> ipsOfNodes, String password) {
      AuAssert.check(!ipsOfNodes.isEmpty());

      logger.info("Setting password for " + clusterName);
      logger.info("Nodes needed to be set password: " + ipsOfNodes.toString());

      ArrayList<String> failedIPs = null;
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      for (String nodeIP : ipsOfNodes) {
         SetVMPasswordSP setVMPasswordSP = new SetVMPasswordSP(nodeIP, password);
         storeProcedures.add(setVMPasswordSP);
      }
      AuAssert.check(!storeProcedures.isEmpty());

      try {
         Callable<Void>[] storeProceduresArray = storeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler.executeStoredProcedures(com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeProceduresArray, callback);

         for (int i = 0; i < storeProceduresArray.length; i++) {
            if (!result[i].finished || result[i].throwable != null) {
               SetVMPasswordSP sp = (SetVMPasswordSP) storeProceduresArray[i];
               String failedNodeIP = sp.getNodeIP();
               if (failedIPs == null) {
                  failedIPs = new ArrayList<String>();
               }
               failedIPs.add(failedNodeIP);
            }
         }
      } catch (Exception e) {
         String errMsg = " : " + e.getMessage();
         logger.error("Error in setting password for " + clusterName + errMsg);
         throw BddException.INTERNAL(e, "Failed to set password for nodes in cluster " + clusterName + errMsg);
      }
      return failedIPs;
   }

   @Override
   public boolean setPasswordForNode(String clusterName, String nodeIP, String password) throws Exception {
      AuAssert.check(clusterName != null && nodeIP != null);

      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      SetVMPasswordSP setVMPasswordSP = new SetVMPasswordSP(nodeIP, password);
      storeProcedures.add(setVMPasswordSP);
      AuAssert.check(!storeProcedures.isEmpty());
      try {
         Callable<Void>[] storeProceduresArray = storeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler.executeStoredProcedures(com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeProceduresArray, callback);

         if (result[0].finished && result[0].throwable == null) {
            return true;
         }
         return false;
      } catch (Exception e) {
         throw BddException.INTERNAL(e, "Failed to set password for " + nodeIP + " in " + clusterName + " : " + e.getMessage());
      }
   }
}
