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

/**
 * Interface for invoke a callback when a task is finished.
 * 
 */
public interface IVcTaskCallback {
   /**
    * Task completion callback.
    * 
    * Callback to be executed by the task manager thread
    * upon task completion. To allow task manager make progress,
    * we shouldn't put blocking operation in this callback.
    * Any blocking operation should be executed by another
    * worker thread.
    */
   public void completeCB(VcTask task);
   
   /**
    * Sync callback.
    * 
    * Callback to be executed by the task waiter to sync with 
    * the jobs launched by {@code completeCB()}.
    */
   public void syncCB();
}
