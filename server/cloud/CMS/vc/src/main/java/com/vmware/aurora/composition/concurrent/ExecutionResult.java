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

package com.vmware.aurora.composition.concurrent;

/**
 * A simple object to hold the result of execution result of a stored procedure, which is
 * executed by a {@link Transaction}.
 *
 * {@link StoredProcedureThreadPoolExecutor} and its background pool threads orchestrate execution of
 * multiple stored procedures in parallel.
 *
 * @author Xin Li (xinli)
 *
 */
public class ExecutionResult {
   final public boolean finished;
   final public Throwable throwable;

   public ExecutionResult(boolean finished, Throwable throwable) {
      this.finished = finished;
      this.throwable = throwable;
   }
}
