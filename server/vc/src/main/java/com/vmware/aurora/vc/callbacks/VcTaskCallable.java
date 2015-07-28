/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.aurora.vc.callbacks;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.IVcTaskCallback;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcTask;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Created By xiaoliangl on 6/11/15.
 */
public abstract class VcTaskCallable implements Callable<VcTask> {
   private final static Logger LOGGER = Logger.getLogger(VcTaskCallable.class);
   private VcTask.TaskType type;
   private IVcTaskCallback callback;
   private VcTaskCallableSequence taskParent;
   private VcTask vcTask;
   private VcObject result;

   public VcTaskCallable(VcTask.TaskType type1, IVcTaskCallback callback1) throws Exception {
      type = type1;
      callback = callback1;
   }

   @Override
   public VcTask call() throws Exception {
      if(LOGGER.isInfoEnabled()) {
         LOGGER.info("start Vc Task Callable:" + type);
      }

      ManagedObjectReference vcTaskRef = callVc();

      vcTask = new VcTask(type, vcTaskRef, getParent(), callback);
      return vcTask;
   }

   public abstract ManagedObjectReference callVc() throws Exception;


   public VcTaskCallableSequence getTaskParent() {
      return taskParent;
   }

   protected void setTaskParent(VcTaskCallableSequence batch1) {
      this.taskParent = batch1;
   }

   public VcTask getVcTask() {
      return vcTask;
   }

   public VcObject getResult() {
      return result;
   }

   public void setResult(VcObject result) {
      this.result = result;
   }

   public VcObject getParent() {
      return null;
   }
}
