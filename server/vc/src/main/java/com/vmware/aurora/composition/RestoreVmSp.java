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

package com.vmware.aurora.composition;

import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcSnapshot;

/**
 * Stored Procedure to restore a VM to one of its snapshots
 *
 * @author Xin Li (xinli)
 *
 */

public class RestoreVmSp implements Callable<Void> {
   private VcSnapshot snapshot;

   public RestoreVmSp(VcSnapshot snapshot) {
      this.snapshot = snapshot;
   }

   @Override
   public Void call() throws Exception {
      snapshot.revert();
      return null;
   }
}
