package com.vmware.bdd.service;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.CmClusterDef;
import com.vmware.bdd.apitypes.CmClusterSpec;
import com.vmware.bdd.service.impl.CmProviderServiceImpl;
import com.vmware.bdd.utils.SerialUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 9:53 AM
 */
public class TestCmProviderServiceImpl {
   private static final Logger logger = Logger.getLogger(TestCmProviderServiceImpl.class);
   private static CmProviderServiceImpl provider;
   private static CmClusterSpec cluster;

   @BeforeClass(groups = { "TestCmProviderServiceImpl" })
   public static void setup() {
      provider = new CmProviderServiceImpl("2.0", 6, 5, "10.141.72.152", 7180, "admin", "admin");
      cluster = new CmClusterSpec();
      cluster.setName("myFirstCluster");
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
   public void testInitializeCluster() throws IOException {
      String content = SerialUtils.dataFromFile("/home/balfox/github/serengeti-ws/server/software-mgmt/src/test/resources/default_cm_cluster.json");
      CmClusterDef cmClusterDef = SerialUtils.getObjectByJsonString(CmClusterDef.class, content);
      System.out.println((new Gson()).toJson(cmClusterDef));

      provider.configure(cmClusterDef);




   }
}
