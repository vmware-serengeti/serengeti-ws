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
package com.vmware.bdd.clone.spec;

import java.util.Map;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.vim.binding.vim.Folder;

/**
 * vm spec
 * 
 * @author tli
 * 
 */
public class VmCreateSpec implements Location {
   private String vmId;

   private String vmName;

   private VmSchema schema;

   private VcResourcePool targetRp;

   private VcDatastore targetDs;

   private VcHost targetHost;

   private Folder targetFolder;

   private Map<String, String> bootupConfigs;

   private boolean linkedClone;

   private IPrePostPowerOn prePowerOn;

   private IPrePostPowerOn postPowerOn;

   public String getVmName() {
      return vmName;
   }

   public void setVmName(String vmName) {
      this.vmName = vmName;
   }

   public String getVmId() {
      return vmId;
   }

   public void setVmId(String vmId) {
      this.vmId = vmId;
   }

   public VmSchema getSchema() {
      return schema;
   }

   public void setSchema(VmSchema schema) {
      this.schema = schema;
   }

   public VcResourcePool getTargetRp() {
      return targetRp;
   }

   public void setTargetRp(VcResourcePool targetRp) {
      this.targetRp = targetRp;
   }

   public VcDatastore getTargetDs() {
      return targetDs;
   }

   public void setTargetDs(VcDatastore targetDs) {
      this.targetDs = targetDs;
   }

   public VcHost getTargetHost() {
      return targetHost;
   }

   public void setTargetHost(VcHost targetHost) {
      this.targetHost = targetHost;
   }

   public Folder getTargetFolder() {
      return targetFolder;
   }

   public void setTargetFolder(Folder targetFolder) {
      this.targetFolder = targetFolder;
   }

   public Map<String, String> getBootupConfigs() {
      return bootupConfigs;
   }

   public void setBootupConfigs(Map<String, String> bootupConfigs) {
      this.bootupConfigs = bootupConfigs;
   }

   public boolean isLinkedClone() {
      return linkedClone;
   }

   public void setLinkedClone(boolean linkedClone) {
      this.linkedClone = linkedClone;
   }

   public IPrePostPowerOn getPrePowerOn() {
      return prePowerOn;
   }

   public void setPrePowerOn(IPrePostPowerOn prePowerOn) {
      this.prePowerOn = prePowerOn;
   }

   public IPrePostPowerOn getPostPowerOn() {
      return postPowerOn;
   }

   public void setPostPowerOn(IPrePostPowerOn postPowerOn) {
      this.postPowerOn = postPowerOn;
   }

   @Override
   public String toString() {
      return "VmCreateSpec [vmName=" + vmName + "]";
   }

   @Override
   public String getLocation() {
      return targetHost.getName();
   }
}
