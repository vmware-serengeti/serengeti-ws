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
package com.vmware.bdd.software.mgmt.plugin.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class InfrastructureException extends SoftwareManagementPluginException {
   private static final long serialVersionUID = 1L;

   private List<String> failedMsgList;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public InfrastructureException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }


   public List<String> getFailedMsgList() {
      return failedMsgList;
   }

   public static InfrastructureException WAIT_VM_STATUS_FAIL(
         String clusterName, List<String> failedMsgList) {
      String details = failedMsgList != null && failedMsgList.size() > 0 ? Arrays.toString(failedMsgList.toArray()) : "";

      InfrastructureException e =
            new InfrastructureException("APP_MANAGER.WAIT_VM_STATUS_FAIL",
                  null, clusterName, details);
      e.failedMsgList = failedMsgList;

      return e;

   }
}
