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

import com.google.gson.internal.Pair;

public class Util {

   /**
    * Get the number of successful execution of stored procedures
    * @param result Execution result returned by {@link Scheduler#executeStoredProcedures(Priority, com.vmware.aurora.tm.IStoredProcedure[])}
    * @return Number of successful execution of stored procedures
    */
   public static int getNumberOfSuccessfulExecution(ExecutionResult[] result) {
      if (result == null) {
         return 0;
      }

      int total = 0;
      for (ExecutionResult r : result) {
         if (r.finished && r.throwable == null) {
            ++ total;
         }
      }
      return total;
   }

   /**
    * Get the number of successful execution of stored procedures
    * @param result Execution result returned by {@link Scheduler#executeStoredProcedures(Priority, Pair[], int)
    * @return Number of successful execution of stored procedures
    */
   public static int getNumberOfSuccessfulExecution(Pair<ExecutionResult, ExecutionResult>[] result) {
      if (result == null) {
         return 0;
      }

      int total = 0;
      for (Pair<ExecutionResult, ExecutionResult> pair : result) {
         if (pair.first.finished && pair.first.throwable == null && pair.second.finished == false) {
            ++ total;
         }
      }
      return total;
   }
}
