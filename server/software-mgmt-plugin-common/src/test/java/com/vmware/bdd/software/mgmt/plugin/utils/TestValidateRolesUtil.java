package com.vmware.bdd.software.mgmt.plugin.utils;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestValidateRolesUtil {
   private String fileDir;
   private NodeGroupInfo groupInfo;

   @BeforeMethod
   public void setup() {
      String ironfanBlackListFile = "/scale_in_roles_blacklist.json";
      String filePath = getClass().getResource(ironfanBlackListFile).getPath();
      File file = new File(filePath);
      fileDir = file.getParent();
      List<String> roles = new ArrayList<String>();
      groupInfo = new NodeGroupInfo();
      groupInfo.setRoles(roles);
   }

   @AfterMethod
   public void tearDown() {
      groupInfo.setRoles(new ArrayList<String>());
   }

   @Test(expectedExceptions = SoftwareManagementPluginException.class)
   public void testValidateRolesForShrinkFailed() {
      groupInfo.getRoles().add("hadoop_namenode");
      ValidateRolesUtil.validateRolesForShrink(fileDir, groupInfo);
   }

   @Test
   public void testValidateRolesForShrinkSucceed() {
      groupInfo.getRoles().add("hadoop_tasktracker");
      ValidateRolesUtil.validateRolesForShrink(fileDir, groupInfo);
   }

}