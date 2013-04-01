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

import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcSnapshot;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

/**
 * Store procedure for taking snap shot of vm.
 */
public class TakeSnapshotSP implements Callable<Void> {

   final String vmId;
   final String name;
   final String description;
   transient String snapId;
   transient VcSnapshot snap;

   public TakeSnapshotSP(String vmId, String name, String description) {
      this.vmId = vmId;
      this.name = name;
      this.description = description;
   }

   @Override
   public Void call() throws Exception {
      final VcVirtualMachine vm = VcCache.get(vmId);
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            VcSnapshot snap = vm.createSnapshot(name, description);
            snapId = snap.getId();
            return null;
         }
         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }

   public String getSnapId() {
      return snapId;
   }

   public VcSnapshot getSnap() {
      return snap;
   }
}
