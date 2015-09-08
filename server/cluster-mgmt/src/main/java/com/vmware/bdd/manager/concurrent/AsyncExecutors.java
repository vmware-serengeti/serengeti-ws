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
package com.vmware.bdd.manager.concurrent;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by xiaoliangl on 9/8/15.
 */
@Component
public class AsyncExecutors {
   public static final String CLUSTER_SYNC_EXEC = "clusterSyncExec";
   public static final String NODE_SYNC_EXEC = "nodeSyncExec";

   static final String SHUTDOWN = "shutdown";

   @Bean(name = CLUSTER_SYNC_EXEC, destroyMethod = SHUTDOWN)
   @Qualifier(CLUSTER_SYNC_EXEC)
   public Executor clusterSyncExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(20);
      executor.setQueueCapacity(200);
      executor.setThreadNamePrefix(CLUSTER_SYNC_EXEC + "-");
      executor.setKeepAliveSeconds(120);
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      executor.initialize();
      return executor;
   }

   @Bean(name = NODE_SYNC_EXEC, destroyMethod = SHUTDOWN)
   @Qualifier(NODE_SYNC_EXEC)
   public Executor nodeSyncExec() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(10);
      executor.setMaxPoolSize(100);
      executor.setQueueCapacity(1000);
      executor.setThreadNamePrefix(NODE_SYNC_EXEC + "-");
      executor.setKeepAliveSeconds(120);
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      executor.initialize();
      return executor;
   }
}
