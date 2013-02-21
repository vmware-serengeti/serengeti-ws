/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.vmware.bdd.cli.commands.ConnectionCommands;

/**
 * This class is the test of connect command.
 */
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class ConnectionCommandsTest extends MockRestServer {
   @Autowired
   private ConnectionCommands connectionCommands;
   
   @Test
   public void testConn() throws Exception {
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/j_spring_security_check?j_username=username&j_password=password",
            HttpMethod.POST, HttpStatus.OK, "");
      connectionCommands.conn("127.0.0.1:8080", "username", "password");
   }
}
