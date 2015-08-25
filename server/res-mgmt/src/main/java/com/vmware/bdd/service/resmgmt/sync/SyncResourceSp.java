/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.resmgmt.sync;

import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.AuAssert;
import org.apache.log4j.Logger;

/**
 * Created by xiaoliangl on 7/16/15.
 */
public class SyncResourceSp extends AbstractSyncVcResSP {
   private static final Logger LOGGER = Logger.getLogger(SyncResourceSp.class);

   private final VcObject vcObject;

   public SyncResourceSp(VcObject vcObject1, boolean skippRefresh) {
      super(skippRefresh);

      AuAssert.check(vcObject1 != null, "can't sync an null Vc Object");

      vcObject = vcObject1;
   }

   @Override
   protected VcObject syncThis() {
      if(isSkipped()) {
         return vcObject;
      }

      //sync the current vc resource object
      return VcContext.inVcSessionDo(new VcSession<VcObject>() {
         @Override
         protected VcObject body() {
            try {
               vcObject.update();
               if(LOGGER.isDebugEnabled()) {
                  LOGGER.debug(String.format("vc resource[%1s-%2s] is updated.", vcObject.getName(), vcObject.getMoRef().getValue()));
               }
            } catch (Exception e) {
               LOGGER.error(String.format("update vc resource[%1s-%2s] failed.", vcObject.getName(), vcObject.getMoRef().getValue()), e);
            }

            return vcObject;
         }
      });
   }

}
