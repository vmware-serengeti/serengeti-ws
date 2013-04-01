/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

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
