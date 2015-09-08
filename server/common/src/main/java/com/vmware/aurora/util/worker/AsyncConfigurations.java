/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.aurora.util.worker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Async Executors for CmsWorker.
 */
@Configuration
public class AsyncConfigurations {
   public static final String VC_CACHE_SCHEDULER="vcCacheScheduler";
   public static final String VC_QUERY_EXEC = "vcQueryExec";
   public static final String VC_CACHE_EXEC = "vcCacheExec";
   static final String SHUTDOWN = "shutdown";

   @Bean(name = VC_QUERY_EXEC, destroyMethod = SHUTDOWN)
   @Qualifier(VC_QUERY_EXEC)
   public Executor vcQueryExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(5);
      executor.setMaxPoolSize(25);
      executor.setQueueCapacity(5000);
      executor.setThreadNamePrefix(VC_QUERY_EXEC + "-");
      executor.initialize();
      return executor;
   }

   @Bean(name = VC_CACHE_EXEC, destroyMethod = SHUTDOWN)
   @Qualifier(VC_CACHE_EXEC)
   public Executor vcCacheExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(1);
      executor.setMaxPoolSize(5);
      executor.setQueueCapacity(1000);
      executor.setThreadNamePrefix(VC_CACHE_EXEC + "-");
      executor.initialize();
      return executor;
   }

   @Bean(name = VC_CACHE_SCHEDULER, destroyMethod = SHUTDOWN)
   @Qualifier(VC_CACHE_SCHEDULER)
   public ScheduledExecutorService vcCacheScheduler() {
      ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(VC_CACHE_SCHEDULER + "-%d").build();

      return Executors.newScheduledThreadPool(2, threadFactory);
   }
}
