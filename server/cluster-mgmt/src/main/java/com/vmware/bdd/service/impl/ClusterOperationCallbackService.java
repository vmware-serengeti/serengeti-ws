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
package com.vmware.bdd.service.impl;

import org.apache.log4j.Logger;

import com.vmware.bdd.service.IClusterOperationCallbackService;
import com.vmware.bdd.service.sp.ClusterOperationCallbackSP;
import com.vmware.bdd.utils.AuAssert;

public class ClusterOperationCallbackService implements IClusterOperationCallbackService {

   private static final Logger logger = Logger.getLogger(ClusterOperationCallbackService.class);

   @Override
   public boolean invoke(String phase, String clusterName, String clusterOperation, String appMgrType, String appMgrVersion, String vendorName, String distroVersion) {
      AuAssert.check(phase != null && clusterName != null && clusterOperation != null && appMgrType != null && appMgrVersion != null && vendorName != null && distroVersion != null);

      ClusterOperationCallbackSP clusterOperationCallbackSP = new ClusterOperationCallbackSP(phase, clusterName, clusterOperation, appMgrType, appMgrVersion, vendorName, distroVersion);
      try {
         if (clusterOperationCallbackSP.invoke()) {
            logger.info(phase + " for cluster " + clusterName + " succeed. ");
            return true;
         }
         return false;
      } catch (Exception e) {
         logger.error(phase + " for cluster " + clusterName + " failed. ", e);
         return false;
      }
   }

}
