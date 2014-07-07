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

import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.v1.CommandsResource;

import java.util.Date;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 10:44 AM
 */
public class FakeCommandsResource implements CommandsResource {
   @Override
   public ApiCommand readCommand(long l) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setEndTime(new Date());
      command.setId(l);
      command.setSuccess(true);
      return command;
   }

   @Override
   public ApiCommand abortCommand(long l) {
      return null;
   }
}
