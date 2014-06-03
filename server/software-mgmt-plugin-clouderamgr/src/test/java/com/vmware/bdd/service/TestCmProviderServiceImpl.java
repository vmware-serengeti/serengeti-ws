package com.vmware.bdd.service;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.CmClusterDef;
import com.vmware.bdd.service.impl.CmProviderServiceImpl;
import com.vmware.bdd.utils.SerialUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 9:53 AM
 */
public class TestCmProviderServiceImpl {
   private static final Logger logger = Logger.getLogger(TestCmProviderServiceImpl.class);
   private static CmProviderServiceImpl provider;

   @BeforeClass(groups = { "TestCmProviderServiceImpl" })
   public static void setup() {
      provider = new CmProviderServiceImpl("2.0", 6, 5, "10.141.72.152", 7180, "admin", "admin");
   }

   @AfterClass(groups = { "TestCmProviderServiceImpl" })
   public static void tearDown() {

   }

   @BeforeMethod(groups = { "TestCmProviderServiceImpl" })
   public void beforeMethod() {

   }

   @AfterMethod(groups = { "TestCmProviderServiceImpl" })
   public void afterMethod() {

   }

   @Test(groups = { "TestCmProviderServiceImpl" })
   public void testInitializeCluster() throws Exception {
      String content = SerialUtils.dataFromFile("/home/balfox/github/serengeti-ws/server/software-mgmt/src/test/resources/default_cm_cluster.json");
      //String content = SerialUtils.dataFromFile("/home/balfox/github/serengeti-ws/server/software-mgmt/src/test/resources/test.json");
      CmClusterDef cmClusterDef = SerialUtils.getObjectByJsonString(CmClusterDef.class, content);
      System.out.println((new Gson()).toJson(cmClusterDef));

      //System.out.println(provider.isProvisioned(cmClusterDef));

      provider.provisionCluster(cmClusterDef);

      //provider.provisionParcels(cmClusterDef);

      //provider.addHosts(cmClusterDef);

      //provider.provisionCluster(cmClusterDef);

      //provider.provisionParcels(cmClusterDef);
      //provider.unconfigureServices(cmClusterDef);
      //provider.deleteHosts(cmClusterDef);
      //provider.stop(cmClusterDef);


      //provider.configure(cmClusterDef);
   }
}
