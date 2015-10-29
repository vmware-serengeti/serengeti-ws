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
package com.vmware.bdd.cli.commands;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.VirtualMachineRead;
import com.vmware.bdd.cli.rest.NodeTemplateRestClient;

@Component
public class NodeTemplateCommands implements CommandMarker {

   @Autowired
   private NodeTemplateRestClient restClient;

   @CliCommand(value = "template list", help = "List all node templates")
   public void list() {
      try {
         VirtualMachineRead[] templates = restClient.list();
         prettyOutputNodeTemplatesInfo(templates);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_TEMPLATE,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   private void prettyOutputNodeTemplatesInfo(VirtualMachineRead[] templates) {
      LinkedHashMap<String, List<String>> columnNamesWithGetMethodNames = new LinkedHashMap<String, List<String>>();
      columnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
      columnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_VM_MOID, Arrays.asList("getMoid"));
      columnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_VM_LAST_MODIFIED, Arrays.asList("getLastModified"));
      columnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_VM_TAG, Arrays.asList("getTag"));
      try {
         CommandsUtils
               .printInTableFormat(columnNamesWithGetMethodNames, templates, Constants.OUTPUT_INDENT);
      } catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }

}
