/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
package com.vmware.bdd.manager.task;

import java.io.Serializable;
import java.util.Map;

public interface TaskListener extends Serializable {
   /**
    * This will be called when the command successes but the last message is not
    * received after max timeout.
    */
   public void onSuccess();

   /**
    * This will be called when the command failed but the last message is not
    * received after max timeout.
    */
   public void onFailure();

   /**
    * This will be called when a new message is received.
    */
   public void onMessage(Map<String, Object> mMap);

   /**
    * Get command array
    */
   public String[] getTaskCommand(String clusterName, String fileName);
}
