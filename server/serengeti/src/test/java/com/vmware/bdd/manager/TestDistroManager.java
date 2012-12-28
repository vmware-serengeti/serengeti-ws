package com.vmware.bdd.manager;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.DistroRead;

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
   public void testGetDistros () {
      List<String> vendors = new ArrayList<String>();
      DistroManager distroManager = new DistroManager();
      List<DistroRead> distros= distroManager.getDistros();
      for(DistroRead dr : distros) {
         vendors.add(dr.getVendor());
      }
      assertTrue("It must contains vendor, Apache.", vendors.contains("Apache"));
      assertTrue("It must contains vendor, Greenplum.", vendors.contains("Greenplum"));
      assertTrue("It must contains vendor, Cloudera.", vendors.contains("Cloudera"));
      assertTrue("It must contains vendor, Hortonworks.", vendors.contains("Hortonworks"));
   }

}
