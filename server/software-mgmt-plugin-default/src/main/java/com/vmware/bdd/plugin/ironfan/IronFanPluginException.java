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
package com.vmware.bdd.plugin.ironfan;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

/**
 * Exceptions from IronFan plugin
 */
public class IronFanPluginException extends SoftwareManagementPluginException {

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public IronFanPluginException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public final static IronFanPluginException GET_ROLES_ERR_EXIT_CODE(int exitCode) {
      throw new IronFanPluginException("IRONFAN.GET_ROLES_ERR_EXIT_CODE", null, exitCode);
   }

   public final static IronFanPluginException GET_ROLES_EXCEPTION(Exception cause) {
      return new IronFanPluginException("IRONFAN.GET_ROLES_EXCEPTION", cause);
   }
}
