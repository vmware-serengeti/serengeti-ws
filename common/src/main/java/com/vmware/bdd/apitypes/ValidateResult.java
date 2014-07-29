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
package com.vmware.bdd.apitypes;

import java.util.ArrayList;
import java.util.List;

public class ValidateResult {

   private boolean validated;
   private List<String> failedMsgList = new ArrayList<String>();
   private List<String> warningMsgList = new ArrayList<String>();

   public boolean isValidated() {
      return validated;
   }
   public void setValidated(boolean validated) {
      this.validated = validated;
   }
   public List<String> getFailedMsgList() {
      return failedMsgList;
   }
   public void setFailedMsgList(List<String> failedMsgList) {
      this.failedMsgList = failedMsgList;
   }
   public List<String> getWarningMsgList() {
      return warningMsgList;
   }
   public void setWarningMsgList(List<String> warningMsgList) {
      this.warningMsgList = warningMsgList;
   }

}
