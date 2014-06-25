/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.monitor;

/**
 * Author: Xiaoding Bian
 * Date: 6/30/14
 * Time: 5:25 PM
 */
public abstract class StatusPoller {

   private int pollPeriodMs = 3000;

   public void waitForComplete() throws Exception {
      setup();
      while (true) {
         if (poll()) {
            break;
         }
         Thread.sleep(pollPeriodMs);
      }
      tearDown();
   }

   public abstract boolean poll();

   public void setup() {}

   public void tearDown() {}

   public void setPollPeriodMs(int pollPeriodMs) {
      this.pollPeriodMs = pollPeriodMs;
   }

   public int getPollPeriodMs() {
      return pollPeriodMs;
   }

}
