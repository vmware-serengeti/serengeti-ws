/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcResourcePool;

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
    * Path to the folder for file testing.
    */
   public static String testFilePath;

   /**
    * Postfix used for creating unique names for VM, RP, etc. in test runs.
    */
   public static String testPostfix;

   public static String testDsName;

   /**
    * Path to the ovf to be uploaded as VM template
    */
   public static String ovfPath = "./src/test/resources/aurora_dbvm-OVF10.ovf";

   public static String dbVersion = "1.0.1000";

   public static VcResourcePool getTestRP() throws Exception {
      VcResourcePool testRP = null;
      for (VcCluster cluster : VcInventory.getClusters()) {
         System.out.println(cluster);
         testRP = cluster.searchRP(VcTestConfig.testRpPath);
         if (testRP != null) {
            break;
         }
      }
      if (testRP == null) {
         throw new Exception("cannot find test rp");
      }
      return testRP;
   }

}
