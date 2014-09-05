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
package com.vmware.bdd.plugin.clouderamgr.exception;

/**
 * Created by admin on 8/6/14.
 */
public class CommandExecFailException extends ClouderaManagerException{

   private String hostId;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public CommandExecFailException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public String getRefHostId() {
      return hostId;
   }

   public static CommandExecFailException EXECUTE_COMMAND_FAIL(String hostId, String refMsg) {
      CommandExecFailException e = new CommandExecFailException("CLOUDERA_MANAGER.EXECUTE_COMMAND_FAIL", null, hostId, refMsg);
      e.hostId = hostId;
      return e;
   }
}
