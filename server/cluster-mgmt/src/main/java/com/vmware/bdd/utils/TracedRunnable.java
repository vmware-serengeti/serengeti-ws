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
package com.vmware.bdd.utils;

import org.apache.log4j.Logger;

public abstract class TracedRunnable implements Runnable {
   private static final Logger logger = Logger.getLogger(TracedRunnable.class);

   @Override
   final public void run() {
      try {
         logger.info("prepare execution: " + this.getClass().getCanonicalName());
         onStart();
         logger.info("execution started: " + this.getClass().getCanonicalName());
         doWork();
         logger.info("cleanup execution: " + this.getClass().getCanonicalName());
         onFinish();
         logger.info("execution succeed: " + this.getClass().getCanonicalName());
      } catch (Throwable t) {
         logger.error("execution failed: " + this.getClass().getCanonicalName(), t);
         onException(t);
      }
   }

   abstract public void doWork() throws Exception;

   abstract public void onStart() throws Exception;

   abstract public void onFinish() throws Exception;

   abstract public void onException(Throwable t);
}
