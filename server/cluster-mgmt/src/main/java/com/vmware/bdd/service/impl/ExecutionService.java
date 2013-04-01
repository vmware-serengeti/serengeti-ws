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
package com.vmware.bdd.service.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.vmware.bdd.service.IExecutionService;

public class ExecutionService implements IExecutionService, InitializingBean,
      DisposableBean {
   private static final Logger logger = Logger.getLogger(ExecutionService.class);

   int poolSize = 10;
   int queueSize = 50;

   ExecutorService executorService;

   public ExecutionService() {
      // setting the default value
      poolSize = 20;
      queueSize = 50;
   }

   public int getPoolSize() {
      return poolSize;
   }

   public void setPoolSize(int poolSize) {
      this.poolSize = poolSize;
   }

   public int getQueueSize() {
      return queueSize;
   }

   public void setQueueSize(int queueSize) {
      this.queueSize = queueSize;
   }

   @Override
   public <V> V execute(Callable<V> task) throws InterruptedException,
         ExecutionException {
      Future<V> future = executorService.submit(task);
      return future.get();
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      logger.info("starting thread pool executor");
      long keepAliveTimeMs = 60000;
      executorService = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTimeMs,
            TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
   }

   @Override
   public void destroy() throws Exception {
      logger.info("shutting down thread pool executor");
      executorService.shutdownNow();
      executorService.awaitTermination(10, TimeUnit.SECONDS);
   }
}
