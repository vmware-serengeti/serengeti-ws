/*****************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.DatastoreAdd;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.DatastoreReadDetail;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.DatastoreRestClient;

@Component
public class DatastoreCommands implements CommandMarker {
   @Autowired
   private DatastoreRestClient restClient;

   @CliAvailabilityIndicator({ "datastore help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "datastore add", help = "Add new datastore(s)")
   public void addDatastore(
         @CliOption(key = { "name" }, mandatory = true, help = "The datastore name.") final String name,
         @CliOption(key = { "spec" }, mandatory = true, help = "The datastore name(s) in vsphere: use \",\" among names.") final String spec,
         @CliOption(key = { "type" }, mandatory = false, unspecifiedDefaultValue = "SHARED", help = "You must specify the type for storage: "
               + "SHARED or LOCAL") final String type,
         @CliOption(key = { "regex" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false",
               help = "Whether use regular expression for the datastore name(s) in vsphere") final boolean regex) {

      //build DatastoreAdd object
      try {
         DatastoreAdd datastoreAdd = new DatastoreAdd();
         datastoreAdd.setName(name);

         if (CommandsUtils.inputsConvert(spec).isEmpty()) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE,
                  name, Constants.OUTPUT_OP_ADD,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.INPUT_SPEC_PARAM
                        + Constants.MULTI_INPUTS_CHECK);
         } else {
            datastoreAdd.setSpec(CommandsUtils.inputsConvert(spec));
            datastoreAdd.setType(DatastoreType.valueOf(type.toUpperCase()));
            if (regex) {
               datastoreAdd.setRegex(true);
            }

            restClient.add(datastoreAdd);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_DATASTORE,
                  name, Constants.OUTPUT_OP_RESULT_ADD);
         }
      } catch (IllegalArgumentException ex) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, name,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.INVALID_VALUE + " " + "type=" + type);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, name,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "datastore delete", help = "Delete an unused datastore")
   public void deleteDatastore(
         @CliOption(key = { "name" }, mandatory = true, help = "The datastore name") final String name) {

      try {
         restClient.delete(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_DATASTORE, name,
               Constants.OUTPUT_OP_RESULT_DELETE);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, name,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   /**
    * <p>
    * Display datastore list. eg. datastore list -name ds1 ,datastore list
    * --detail
    * </p>
    *
    * @param name
    *           The datastore name
    * @param detail
    *           The datastore detail information
    */
   @CliCommand(value = "datastore list", help = "Display datastore list.")
   public void listDatastore(
         @CliOption(key = { "name" }, mandatory = false, help = "The datastore name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The datastore detail information") final boolean detail) {
      // rest invocation
      try {
         if (CommandsUtils.isBlank(name)) {
            DatastoreRead[] datastores = restClient.getAll();
            if (datastores != null) {
               prettyOutputDatastoresInfo(datastores, detail);
            }
         } else {
            DatastoreRead datastore = restClient.get(name);
            if (datastore != null) {
               prettyOutputDatastoreInfo(datastore, detail);
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, name,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   private void prettyOutputDatastoresInfo(DatastoreRead[] datastores,
         boolean detail) {
      if (datastores != null) {
         LinkedHashMap<String, List<String>> datastoreColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         datastoreColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         datastoreColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_TYPE, Arrays.asList("getType"));
         datastoreColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_REGEX, Arrays.asList("getRegex"));
         //TODO when backend support it, we can remove the comment.
         //         datastoreColumnNamesWithGetMethodNames.put(
         //               Constants.FORMAT_TABLE_COLUMN_SPACE,
         //               Arrays.asList("getTotalSpaceGB"));
         //         datastoreColumnNamesWithGetMethodNames.put(
         //               Constants.FORMAT_TABLE_COLUMN_FREE_SPACE,
         //               Arrays.asList("getFreeSpaceGB"));

         try {
            if (detail) {
               List<DatastoreReadDetail> datastoreReadDetails = null;
               LinkedHashMap<String, List<String>> datastoreDetailColumnNamesWithGetMethodNames =
                     new LinkedHashMap<String, List<String>>();
               datastoreDetailColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_NAME,
                     Arrays.asList("getVcDatastoreName"));
               //TODO when backend support it, we can remove the comment.
               //               datastoreDetailColumnNamesWithGetMethodNames.put(
               //                     Constants.FORMAT_TABLE_COLUMN_HOST,
               //                     Arrays.asList("getHost"));
               //               datastoreDetailColumnNamesWithGetMethodNames.put(
               //                     Constants.FORMAT_TABLE_COLUMN_SPACE,
               //                     Arrays.asList("getTotalStorageSizeGB"));
               //               datastoreDetailColumnNamesWithGetMethodNames.put(
               //                     Constants.FORMAT_TABLE_COLUMN_FREE_SPACE,
               //                     Arrays.asList("getFreeSpaceGB"));
               for (DatastoreRead datastore : datastores) {
                  if (datastore != null) {
                     CommandsUtils.printInTableFormat(
                           datastoreColumnNamesWithGetMethodNames,
                           new DatastoreRead[] { datastore },
                           Constants.OUTPUT_INDENT);
                     datastoreReadDetails = datastore.getDatastoreReadDetails();
                     if (datastoreReadDetails != null) {
                        System.out.println();
                        CommandsUtils
                              .printInTableFormat(
                                    datastoreDetailColumnNamesWithGetMethodNames,
                                    datastoreReadDetails.toArray(),
                                    new StringBuilder()
                                          .append(Constants.OUTPUT_INDENT)
                                          .append(Constants.OUTPUT_INDENT)
                                          .toString());
                     }
                  }
                  System.out.println();
               }
            } else {
               CommandsUtils.printInTableFormat(
                     datastoreColumnNamesWithGetMethodNames, datastores,
                     Constants.OUTPUT_INDENT);
            }
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }
   }


   private void prettyOutputDatastoreInfo(DatastoreRead datastore,
         boolean detail) {
      if (datastore != null)
         prettyOutputDatastoresInfo(new DatastoreRead[] { datastore }, detail);
   }

}
