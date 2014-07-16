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
package com.vmware.bdd.plugin.clouderamgr.model;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;
import com.vmware.vim.binding.vmodl.displayName;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:56 PM
 */
public class CmRoleDef extends AbstractCmServiceRole{

   @Expose
   private String nodeRef;

   private boolean isActive; // only useful for NN HA so far

   public String getNodeRef() {
      return nodeRef;
   }

   public void setNodeRef(String nodeRef) {
      this.nodeRef = nodeRef;
   }

   public boolean isActive() {
      return isActive;
   }

   public void setActive(boolean isActive) {
      this.isActive = isActive;
   }

   @Override
   public boolean isService() {
      return false;
   }

   @Override
   public boolean isRole() {
      return true;
   }
}
