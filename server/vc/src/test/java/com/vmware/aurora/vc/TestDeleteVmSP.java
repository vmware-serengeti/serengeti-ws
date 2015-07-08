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

package com.vmware.aurora.vc;

import org.testng.annotations.Test;

import com.vmware.aurora.composition.DeleteVmSP;
import com.vmware.aurora.vc.AbstractTmTest;
import com.vmware.aurora.vc.VcVirtualMachine;

public class TestDeleteVmSP extends AbstractTmTest {

   @Test
   public void testDeleteVm() throws Exception {
      String vmName = "clonedVM1";
      // Need to import the VM first, since TM layer doesn't persist data.
      VcVirtualMachine vm = util.testImportVM(vmName, rp).getResult();

      DeleteVmSP sp = new DeleteVmSP(vm);
      sp.call();
   }
}
