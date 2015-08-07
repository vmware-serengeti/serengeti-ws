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

package com.vmware.bdd.service.resmgmt;

import java.util.List;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.VirtualMachineRead;

public interface INodeTemplateService {

   public List<VirtualMachineRead> getAllNodeTemplates();
   public VcVirtualMachine getNodeTemplateVMByMoid(String moid);
   public VcVirtualMachine getNodeTemplateVMByName(String templateName);
   public String getNodeTemplateNameByMoid(String moid);
   public String getNodeTemplateIdByName(String templateName);

}
