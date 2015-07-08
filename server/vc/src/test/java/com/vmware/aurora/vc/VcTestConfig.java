/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.vc;

import java.util.Properties;

import com.google.gson.internal.Pair;

/**
 * Configurations for junit testing. The global variables are setup by settings
 * in test.properties. To avoid conflicts in running junit tests, it's better
 * to override the default settings.
 */
public class VcTestConfig {
   /**
    * Path to resource pool used for testing. It's expected to be pre-created in VC.
    */
   public static String testRpPath;

   /**
    * Postfix used for creating unique names for VM, RP, etc. in test runs.
    */
   public static String testPostfix;

   public static String testDsName;

   /**
    * Path to the ovf to be uploaded as VM template
    */
   public static String ovfPath = "./src/test/resources/aurora_dbvm-OVF10.ovf";

   public static String testVmFolderName;

   static {
      try {
         init();
      } catch (Exception e) {
         System.out.println("VcTestConfig init failure");
         e.printStackTrace();
      }
   }

   public static Pair<VcDatacenter, VcResourcePool> getTestRPAndDC() throws Exception {
      VcResourcePool testRP = null;
      VcDatacenter testDC = null;

      for(VcDatacenter dc : VcInventory.getDatacenters()) {
         testDC = dc;
         for(VcCluster cluster : dc.getVcClusters()) {
            testRP = cluster.searchRP(VcTestConfig.testRpPath);
            if (testRP != null) {
               break;
            }
         }
      }

      if (testRP == null) {
         throw new Exception("cannot find test rp:" + VcTestConfig.testRpPath);
      }
      return new Pair<VcDatacenter, VcResourcePool>(testDC, testRP);
   }

   public static VcDatastore getTestDS() throws Exception {
      for (VcCluster cluster : VcInventory.getClusters()) {
         for (VcDatastore ds : cluster.getAllDatastores()) {
            if (ds.getName().equals(testDsName)) {
               return ds;
            }
         }
      }
      throw new Exception("cannot find test ds");
   }

   public static void init() throws Exception {
      Properties properties = new Properties();
      FileInputStream in = new FileInputStream("./src/test/resources/test.properties");
      properties.load(in);
      in.close();

      testRpPath = properties.getProperty("testRpPath");
      testPostfix = properties.getProperty("testPostfix");
      testDsName = properties.getProperty("testDsName");
      testVmFolderName = properties.getProperty("testVmFolderName");
   }
}
