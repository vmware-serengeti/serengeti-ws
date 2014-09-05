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
package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.model.ApiEcho;
import com.cloudera.api.v1.ToolsResource;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 8/27/14
 * Time: 3:59 PM
 */
public class FakeToolsResource implements ToolsResource {
   @Override
   public ApiEcho echo(@DefaultValue("Hello, World!") String s) {
      ApiEcho echo = new ApiEcho();
      echo.setMessage(s);
      return echo;
   }

   @Override
   public ApiEcho echoError(@DefaultValue("Default error message") String s) {
      return null;
   }
}
