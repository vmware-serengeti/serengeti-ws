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
package com.vmware.bdd.software.mgmt.plugin.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vmware.bdd.apitypes.ClusterCreate;

public class TestFileUtil {
   private static final Logger logger = Logger
         .getLogger(TestFileUtil.class);

   public static final String HDFS_HA_CLUSTER_FILE =
         "src/test/resources/hdfsHA.json";

   private static String readJson(String fileName) {
      File file = new File(fileName);

      StringBuffer json = new StringBuffer();
      try {
         BufferedReader reader = new BufferedReader(new FileReader(file));
         String line;
         while ((line = reader.readLine()) != null) {
            json.append(line.trim());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return json.toString();
   }

   public static ClusterCreate getSimpleClusterSpec(String specName)
         throws Exception {
      String json = TestFileUtil.readJson(specName);
      logger.info(json);

      ObjectMapper mapper = new ObjectMapper();

      try {
         return mapper.readValue(json, ClusterCreate.class);
      } catch (Exception e) {
         logger.error(e.getMessage());
         throw e;
      }
   }
}