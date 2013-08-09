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
package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler.ProgressCallback;
import com.vmware.bdd.service.job.StatusUpdater;

public class BaseProgressCallback implements ProgressCallback {
   private static final Logger logger = Logger
      .getLogger(BaseProgressCallback.class);
   private StatusUpdater statusUpdator;
   private int successNum = 0;
   private int failureNum = 0;
   private int minPercent = 0;
   private int maxPercent = 0;

   public BaseProgressCallback(StatusUpdater statusUpdator) {
      this.statusUpdator = statusUpdator;
      this.maxPercent = 100;
      this.minPercent = 0;
   }

   public BaseProgressCallback(StatusUpdater statusUpdator, int min, int max) {
      this.statusUpdator = statusUpdator;
      this.maxPercent = max;
      this.minPercent = min;
   }

   @Override
   public void progressUpdate(Callable<Void> sp, ExecutionResult result,
         boolean compensate, int total) {
      if (compensate) {
         // if compensate, do not update progress, for the forward progress has gone to 100%.
         // Suppose no compensate job in Serengeti also
         return;
      }

      if (result.throwable != null) {
         // failed
         failureNum ++;
      } else {
         successNum ++;
      }
      int progress = ((failureNum + successNum)) * 100 / total; // this step progress
      progress = ((maxPercent - minPercent) * progress) / 100 + minPercent; // get value match into min ~ max scope
      double progressd = (double) progress / 100;
      statusUpdator.setProgress(progressd);
      logger.info("set step progress: " + progressd);
   }
}
