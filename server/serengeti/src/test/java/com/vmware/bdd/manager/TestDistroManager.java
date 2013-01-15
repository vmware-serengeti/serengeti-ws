package com.vmware.bdd.manager;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.utils.Constants;

public class TestDistroManager {

   public void testGetDistrosFailure() {
      List<String> vendors = new ArrayList<String>();
      try {
         DistroManager distroManager = new DistroManager();
         List<DistroRead> distros = distroManager.getDistros();
         for (DistroRead dr : distros) {
            vendors.add(dr.getVendor());
         }
         fail("creating cluster");
      } catch (Exception e) {

      }
      assertTrue("vendors must be empty.", vendors.isEmpty());
   }

   @Test
   public void testGetDistros() {
      List<String> vendors = new ArrayList<String>();
      DistroManager distroManager = new DistroManager();
      List<DistroRead> distros = distroManager.getDistros();
      for (DistroRead dr : distros) {
         vendors.add(dr.getVendor());
      }
      assertTrue("It must contains vendor, " + Constants.DEFAULT_VENDOR + ".",
            vendors.contains(Constants.DEFAULT_VENDOR));
      assertTrue("It must contains vendor, " + Constants.GPHD_VENDOR + ".",
            vendors.contains(Constants.GPHD_VENDOR));
      assertTrue("It must contains vendor, " + Constants.CDH_VENDOR + ".",
            vendors.contains(Constants.CDH_VENDOR));
      assertTrue("It must contains vendor, " + Constants.HDP_VENDOR + ".",
            vendors.contains(Constants.HDP_VENDOR));
   }

}
