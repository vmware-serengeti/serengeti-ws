/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.cli.commands.DistroCommands;

@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class DistroCommandsTest extends MockRestServer {
   @Autowired
   private DistroCommands distroCommands;

   @Test
   public void testDistroList() throws Exception {
      DistroRead[] distros = new DistroRead[2];

      DistroRead distro1 = new DistroRead();
      distro1.setName("Apache");
      List<String> roles1 = new ArrayList<String>();
      roles1.add("NameNode");
      roles1.add("JobTracker");
      roles1.add("DataNode");
      roles1.add("HadoopClient");
      distro1.setRoles(roles1);

      DistroRead distro2 = new DistroRead();
      distro2.setName("GP");
      List<String> roles2 = new ArrayList<String>();
      roles2.add("NameNode");
      roles2.add("JobTracker");
      roles2.add("DataNode");
      roles2.add("HadoopClient");
      roles2.add("PIG");
      roles2.add("HIVE");

      distro2.setRoles(roles2);

      distros[0] = distro1;
      distros[1] = distro2;
      
      ObjectMapper mapper = new ObjectMapper();
      
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/distros",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(distros));

      //get all distros
      distroCommands.getDistro(null, false);
   }
   
   @Test
   public void testDistroListFailure() throws Exception {
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/distros",
            HttpMethod.GET, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

      //get all distros
      distroCommands.getDistro(null, false);
   }
}
