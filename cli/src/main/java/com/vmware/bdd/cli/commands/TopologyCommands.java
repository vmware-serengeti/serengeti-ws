/*****************************************************************************
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
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.RackInfo;
import com.vmware.bdd.cli.rest.TopologyRestClient;

@Component
public class TopologyCommands implements CommandMarker {
   @Autowired
   private TopologyRestClient topologyRestClient;

   @CliCommand(value = "topology upload", help = "Upload a rack-->hosts mapping topology file")
   public void upload(@CliOption(key = { "fileName" }, mandatory = true, help = "The topology file name") final String fileName) {
      try {
         List<RackInfo> racks = readRackInfoFromFile(fileName);
         if (!duplicatedNameCheck(racks)) {
            topologyRestClient.upload(racks);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_TOPOLOGY, null,
               Constants.OUTPUT_OP_RESULT_UPLOAD);
         }
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_TOPOLOGY, null, Constants.OUTPUT_OP_UPLOAD,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         return;
      }
   }

   private boolean duplicatedNameCheck(List<RackInfo> racks) {
      Set<String> checkRack = new TreeSet<String>();
      Set<String> checkHosts = new TreeSet<String>();
      for (RackInfo rack: racks) {
         if (checkRack.contains(rack.getName())) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_TOPOLOGY, null, Constants.OUTPUT_OP_UPLOAD,
                  Constants.OUTPUT_OP_RESULT_FAIL, "please remove duplicated racks.");
            return true;
         }
         checkRack.add(rack.getName());

         for (String hostName: rack.getHosts()) {
            if (checkHosts.contains(hostName)) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_TOPOLOGY, null, Constants.OUTPUT_OP_UPLOAD,
                     Constants.OUTPUT_OP_RESULT_FAIL, "please remove duplicated hosts.");
               return true;
            }
            checkHosts.add(hostName);
         }
      }
      return false;
   }

   /*
    * The rack format is: rack_name: hostname1, hostname2,...
    */
   private List<RackInfo> readRackInfoFromFile(String filePath) throws IOException,
   FileNotFoundException, CliException {
      FileReader fileReader = null;
      BufferedReader reader = null;
      List<RackInfo> racksInfo = new ArrayList<RackInfo>();

      try {
         File f = new File(filePath);
         fileReader = new FileReader(f);
         reader = new BufferedReader(fileReader);
         String line = "";
         int lineNum = 1;
         while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
               lineNum++;
               continue;
            } else {
               String[] rackHosts = line.split(":");
               if (rackHosts.length != 2 || rackHosts[0].trim().isEmpty() || rackHosts[1].trim().isEmpty()) {
                  throw new CliException("wrong topology format at line " + lineNum + ".\n" + Constants.TOPLOGY_FORMAT);
               } else {
                  String[] hosts = rackHosts[1].split(",");
                  int numOfNonEmptyHosts = 0;
                  for (int i = 0; i < hosts.length; i++) {
                     hosts[i] = hosts[i].trim();
                     if (!hosts[i].isEmpty()) {
                        hosts[numOfNonEmptyHosts++] = hosts[i]; 
                     }
                  }
                  if (numOfNonEmptyHosts == 0) {
                     throw new CliException("wrong topology format at line " + lineNum + ".\n" + Constants.TOPLOGY_FORMAT);
                  }
                  lineNum++;
                  RackInfo rackInfo = new RackInfo();
                  rackInfo.setName(rackHosts[0].trim());
                  rackInfo.setHosts(Arrays.asList(hosts).subList(0, numOfNonEmptyHosts));
                  racksInfo.add(rackInfo);
               }
            }
         }
      } finally {
         if (reader != null) {
            reader.close();
         }
         if (fileReader != null) {
            fileReader.close();
         }
      }
      return racksInfo;
   }

   @CliCommand(value = "topology list", help = "List a rack-->hosts mapping topology")
   public void list() {
      try {
         RackInfo[] racksInfo = topologyRestClient.list();
         prettyOutputRackInfo(racksInfo);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_TOPOLOGY, null,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   private void prettyOutputRackInfo(RackInfo[] racks) throws Exception {
      if (racks != null) {
         LinkedHashMap<String, List<String>> rackInfoColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         rackInfoColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_RACK, Arrays.asList("getName"));
         rackInfoColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_HOSTS, Arrays.asList("getHosts"));

         CommandsUtils.printInTableFormat(
               rackInfoColumnNamesWithGetMethodNames, racks,
               Constants.OUTPUT_INDENT);
      }
   }
}