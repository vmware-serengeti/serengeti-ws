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
package com.vmware.bdd.service.sp;

import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

public class DeleteRpSp implements Callable<Void> {
   final private VcResourcePool vcRp;
   final private String deleteRpName;

   public DeleteRpSp(VcResourcePool vcRp, String deleteRpName) {
      this.vcRp = vcRp;
      this.deleteRpName = deleteRpName;
   }

   public VcResourcePool getVcRp() {
      return vcRp;
   }

   public String getDeleteRpName() {
      return deleteRpName;
   }

   @Override
   public Void call() throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            List<VcResourcePool> rps = vcRp.getChildren();
            VcResourcePool delete = null;
            for (VcResourcePool rp : rps) {
               if (rp.getName().equals(deleteRpName)) {
                  delete = rp;
               }
            }
            if (delete != null) {
               delete.destroy();
            }
            return null;
         }
         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }
}
