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

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

/**
 * Created By xiaoliangl on 6/12/15.
 */
public class VcTaskCallableSequence {
   private final static Logger LOGGER = Logger.getLogger(VcTaskCallableSequence.class);

   private Queue<VcTaskCallable> taskCallableQueue = new LinkedList<>();
//   private Map<Integer, VcTaskCallable> taskToCallableMap = new HashMap<>();

   public void addStep(VcTaskCallable step){
      if(LOGGER.isInfoEnabled()) {
         LOGGER.info("add a step " + step);
      }

      taskCallableQueue.offer(step);
      step.setTaskParent(this);
   }

//   public void taskStarted(int taskMoref){
//      taskToCallableMap.put(taskMoref, taskCallableQueue.poll());
//   }

   public Queue<VcTaskCallable> getTaskCallableQueue(){ return taskCallableQueue;}
}
