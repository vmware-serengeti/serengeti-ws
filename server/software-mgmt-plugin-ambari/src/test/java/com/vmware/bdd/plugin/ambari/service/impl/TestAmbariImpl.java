package com.vmware.bdd.plugin.ambari.service.impl;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.service.AmbariImpl;

import java.io.IOException;

public class TestAmbariImpl {
   private static final Logger logger = Logger.getLogger(TestAmbariImpl.class);
   private static AmbariImpl provider;

   @BeforeClass(groups = { "TestAmbariImpl" })
   public static void setup() {
      provider = new AmbariImpl("10.141.73.103", 8080, "admin", "admin", null);
   }

   @AfterClass(groups = { "TestAmbariImpl" })
   public static void tearDown() {

   }

   @BeforeMethod(groups = { "TestAmbariImpl" })
   public void beforeMethod() {

   }

   @AfterMethod(groups = { "TestAmbariImpl" })
   public void afterMethod() {

   }

   @Test(groups = { "TestAmbariImpl" })
   public void testInitializeCluster() throws IOException {

   }
}
