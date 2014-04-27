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
package com.vmware.bdd.software.mgmt.plugin.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>SoftwareManagementPluginException</code> is the superclass of those
 * exceptions that can be thrown during the normal operation of the software
 * management plugin.
 * <p>
 * 
 **/
public class SoftwareManagementPluginException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   private String errCode;
   private List<String> failedMsgList = new ArrayList<String>();
   private List<String> warningMsgList = new ArrayList<String>();

   public SoftwareManagementPluginException() {
      super();
   }

   public SoftwareManagementPluginException(String errCode, String message,
         Throwable cause) {
      super(message, cause);
      this.errCode = errCode;
   }

   public List<String> getFailedMsgList() {
       return failedMsgList;
   }

   public List<String> getWarningMsgList() {
       return warningMsgList;
   }
   public String getErrCode() {
      return errCode;
   }
}
