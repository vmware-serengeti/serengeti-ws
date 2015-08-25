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

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcObjectImpl;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import org.apache.log4j.Logger;

/**
 * Created by xiaoliangl on 7/16/15.
 */
public class SyncVcResourceSp extends AbstractSyncVcResSP {
   private static final Logger LOGGER = Logger.getLogger(SyncResourceSp.class);

   private final ManagedObjectReference moRef;

   public SyncVcResourceSp(ManagedObjectReference moRef1) {
      super(false);
      moRef = moRef1;
   }

   @Override
   protected VcObject syncThis() {
      return VcContext.inVcSessionDo(new VcSession<VcObject>() {
         public VcObject body() throws Exception {
            VcObject vcObject = null;
            try {
               vcObject = VcObjectImpl.loadFromMoRef(moRef);

               VcCache.put(moRef, vcObject);

               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug(String.format("vc resource[%1s-%2s] is retrieved.", vcObject.getName(), moRef.getValue()));
               }
            } catch (Exception ex) {
               LOGGER.error(String.format("retrieve vc resource[%1s] failed.", moRef.getValue()), ex);
            }

            return vcObject;
         }
      });
   }
}
