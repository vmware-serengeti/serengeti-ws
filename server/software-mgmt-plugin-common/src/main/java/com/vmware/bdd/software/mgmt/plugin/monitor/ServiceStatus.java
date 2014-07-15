/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

public enum ServiceStatus {
   INSTALLATION_FAILED ( "Installation Failed" ),
   CONFIGURATION_FAILED ( "Configuration Failed" ),
   STARTUP_FAILED ( "Startup Failed" ),
   RUNNING ( "Running" ),
   FAILED ( "Failed" ),
   STOP_FAILED( "Stop Failed"),
   STOP_SUCCEED( "Stop Succeed"),
   STARTING( "Starting" ),
   STARTED( "Started" );
   private String description;
   private ServiceStatus(String description) {
      this.description = description;
   }
   public String toString() {
      return description;
   }
}
