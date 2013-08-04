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

package com.vmware.aurora.vc.vcservice;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTaskMgr;

/**
 * Definition of a VC session.
 *
 *   This class can only be accessed in {@link com.vmware.aurora.vc.vcservice}
 */

public abstract class VcSession<T> {
   /**
    * Initial wait time in milliseconds.
    */
   protected static final long VC_INIT_WAIT_TIME = 1000;
   
   /**
    * On VC connection failures, number of retries before giving up.
    */
   protected int retries = 5;

   /**
    * Number of retries before trying to restart VC login.
    */
   protected int reconnectThreshold = 3;

   protected VcTaskMgr taskMgr; 
   
   protected VcSession() {
      taskMgr = VcContext.getGlobalTaskMgr();
   }
   
   /**
    * Only task session can make VC changes
    */
   protected boolean isTaskSession() { 
      return false;
   }
   
   /**
    * @return the session task manager
    */
   protected VcTaskMgr getTaskMgr() {
      AuAssert.check(isTaskSession());
      return taskMgr;
   }
   
   /**
    * Template function for holding VC session body.
    */
   abstract protected T body() throws Exception;

   /**
    * Undo the VC session operation.
    */
   protected void undo() throws Exception { }
}
