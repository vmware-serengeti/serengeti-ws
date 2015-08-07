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
