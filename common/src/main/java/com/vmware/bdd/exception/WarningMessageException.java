/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.exception;

import java.util.List;

/**
 * Created by jiahuili on 7/14/15.
 */
public class WarningMessageException extends BddException {

   private static final long serialVersionUID = 5011637701410867484L;
   private boolean isWarningMsg = true;

   public WarningMessageException(String msg) {
      super(msg);
   }

   public WarningMessageException(Throwable cause, String section, String errorId,
         Object... detail) {
      super(cause, section, errorId, detail);
   }

   public boolean isWarningMsg() {
      return isWarningMsg;
   }

   public void setWarningMsg(boolean isWarningMsg) {
      this.isWarningMsg = isWarningMsg;
   }

   public static WarningMessageException NEW_DATASTORES_EXCLUDE_OLD_DATASTORES(Throwable ex, String oldDS,
         String newDS) {
      return new WarningMessageException(ex, "CLUSTER_UPDATE", "NEW_DS_EXCLUDE_OLD_DS", oldDS, newDS);
   }

   public static WarningMessageException SET_EMPTY_DATASTORES_TO_NON_EMTPY(Throwable ex, String newDS) {
      return new WarningMessageException(ex, "CLUSTER_UPDATE", "SET_EMPTY_DS_TO_NON_EMTPY", newDS);
   }

}
