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
package com.vmware.bdd.plugin.ambari.api.model;

public enum BootstrapStatus {

   RUNNING(0),
   SUCCESS(1),
   ERROR(2);

   private final int status;

   private BootstrapStatus(int status) {
      this.status = status;
   }

   /**
    * Indicates whether or not it is a valid failure state.
    *
    * @return true if this is a valid failure state.
    */
   public boolean isFailedState() {
      switch (BootstrapStatus.values()[this.status]) {
      case ERROR:
         return true;
      default:
         return false;
      }
   }

   /**
    * Indicates whether or not this is a completed state. Completed means that
    * the associated task has stopped running because it has finished
    * successfully or has failed.
    *
    * @return true if this is a completed state.
    */
   public boolean isCompletedState() {
      switch (BootstrapStatus.values()[this.status]) {
      case SUCCESS:
      case ERROR:
         return true;
      default:
         return false;
      }
   }
}
