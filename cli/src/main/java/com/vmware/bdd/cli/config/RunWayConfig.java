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
package com.vmware.bdd.cli.config;

public class RunWayConfig {

   public enum RunType {
      MANUAL, AUTO
   }

   private static RunType runType = RunType.MANUAL;

   public static RunType getRunType() {
      return runType;
   }

   public static void manualRun() {
      init(RunType.MANUAL);
   }

   public static void autoRun() {
      init(RunType.AUTO);
   }

   private static void init(RunType runType) {
      RunWayConfig.runType = runType;
   }

}
