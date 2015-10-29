/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
