/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.apitypes;

/**
 * Created By xiaoliangl on 11/25/14.
 */
public class AddUserMgmtServerResult {
   private boolean successful;
   private int taskId;
   private String details;

   public boolean isSuccessful() {
      return successful;
   }

   public void setSuccessful(boolean successful) {
      this.successful = successful;
   }

   public int getTaskId() {
      return taskId;
   }

   public void setTaskId(int taskId) {
      this.taskId = taskId;
   }

   public String getDetails() {
      return details;
   }

   public void setDetails(String details) {
      this.details = details;
   }
}
