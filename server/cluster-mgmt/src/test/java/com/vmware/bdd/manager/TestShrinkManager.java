package com.vmware.bdd.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestShrinkManager {
   private ShrinkManager shrinkManager;
   @BeforeMethod
   public void setUp() throws Exception {

   }

   @AfterMethod
   public void tearDown() throws Exception {

   }

   @Test
   public void testShrinkNodeGroup() throws Exception {
   }

   public ShrinkManager getShrinkManager() {
      return shrinkManager;
   }

   @Autowired
   public void setShrinkManager(ShrinkManager shrinkManager) {
      this.shrinkManager = shrinkManager;
   }
}