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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Class PriorityFutureTask extends <tt>FutureTask</tt> with priority information.
 *
 * @author Xin Li (xinli)
 * @see FutureTask
 */

class PriorityFutureTask<T> extends FutureTask<T> {
   private Priority priority;

   public PriorityFutureTask(Callable<T> callable, Priority priority) {
      super(callable);
      this.priority = priority;
   }

   public Priority getPriority() {
      return priority;
   }
}
