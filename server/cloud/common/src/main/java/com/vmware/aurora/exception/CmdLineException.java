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
package com.vmware.aurora.exception;

@SuppressWarnings("serial")
public class CmdLineException extends AuroraException {
   // Default public constructor. This should only be used by AMF client.
   public CmdLineException() {}

   private CmdLineException(Throwable t, String errorid, Object ... args) {
      super(t, "CMDLINE", errorid, args);
   }

   public static CmdLineException COMMAND_ERROR(String errMsg) {
      return new CmdLineException(null, "COMMAND_ERROR", errMsg);
   }

   public static CmdLineException EXEC_ERROR(Throwable t, String cmd) {
      return new CmdLineException(t, "EXEC_ERROR", cmd);
   }

   public static CmdLineException INTERRUPTED(Throwable t, String cmd) {
      return new CmdLineException(t, "INTERRUPTED", cmd);
   }

   public static CmdLineException TIMEOUT(long secondsTaken, String cmd) {
      return new CmdLineException(null, "TIMEOUT", secondsTaken, cmd);
   }
}
