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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by xiaoliangl on 9/7/15.
 */
@Configuration
public class AsyncConfigurations {

   @Bean(name = "vcQueryExec", destroyMethod = "shutdown")
   @Qualifier("vcQueryExec")
   public Executor vcQueryExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(5);
      executor.setMaxPoolSize(25);
      executor.setQueueCapacity(5000);
      executor.setThreadNamePrefix("vcQueryExec-");
      executor.initialize();
      return executor;
   }

   @Bean(name = "vcCacheExec", destroyMethod = "shutdown")
   @Qualifier("vcCacheExec")
   public Executor vcCacheExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(1);
      executor.setMaxPoolSize(5);
      executor.setQueueCapacity(1000);
      executor.setThreadNamePrefix("vcCacheExec-");
      executor.initialize();
      return executor;
   }

   @Bean(name = "vcCacheScheduler", destroyMethod = "shutdown")
   @Qualifier("vcCacheScheduler")
   public ScheduledExecutorService vcCacheScheduler() {
      return Executors.newScheduledThreadPool(1);
   }
}
