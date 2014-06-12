package com.vmware.bdd.apitypes;

import com.google.gson.Gson;
import com.vmware.bdd.model.CmServiceRoleType;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 10:42 AM
 */
public class TestCmServiceRoleType {

   @Test
   public void testServiceRole() {
      System.out.println(CmServiceRoleType.allServices());
      System.out.println(CmServiceRoleType.allRoles());
      System.out.println(CmServiceRoleType.serviceOfRole("NAMENODE"));
      System.out.println(CmServiceRoleType.allRolesOfService("HDFS"));
      System.out.println((new Gson()).toJson(CmServiceRoleType.values()));
   }

}
