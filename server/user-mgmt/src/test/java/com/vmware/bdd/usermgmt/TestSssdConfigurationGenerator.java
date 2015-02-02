/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;

/**
 * Created By xiaoliangl on 12/31/14.
 */
public class TestSssdConfigurationGenerator {
   private SssdConfigurationGenerator sssdConfigurationGenerator;


   @BeforeClass
   public void setup() throws IOException {
      String tmpDirPath = System.getProperty("java.io.tmpdir");

      System.setProperty("serengeti.home.dir.bak", System.getProperty("serengeti.home.dir"));
      System.setProperty("serengeti.home.dir", tmpDirPath);

      File usermgmrConfDir = new File(System.getProperty("serengeti.home.dir") + File.separator + "conf"
            + File.separator + "usermgmt");
      usermgmrConfDir.mkdirs();
      usermgmrConfDir.deleteOnExit();

      File tmpFile = new File(usermgmrConfDir, "sssd.conf.template.LDAP");
      tmpFile.createNewFile();
      tmpFile.deleteOnExit();
      FileCopyUtils.copy(readResource("sssd.conf.template.LDAP"), new FileWriter(tmpFile));

      tmpFile = new File(usermgmrConfDir, "sssd.conf.template.AD_AS_LDAP");
      tmpFile.createNewFile();
      tmpFile.deleteOnExit();
      FileCopyUtils.copy(readResource("sssd.conf.template.AD_AS_LDAP"), new FileWriter(tmpFile));

      sssdConfigurationGenerator = new SssdConfigurationGenerator();
   }

   @AfterClass
   public void teardown() {
      System.setProperty("serengeti.home.dir", System.getProperty("serengeti.home.dir.bak"));
      System.out.println(System.getProperty("serengeti.home.dir"));
   }

   public String readResource(String fileName) throws IOException {
      InputStream templateResStream = TestSssdConfigurationGenerator.class.getResourceAsStream("/com/vmware/bdd/usermgmt/" + fileName);


      StringBuilder stringBuilder = new StringBuilder();
      try (BufferedReader templateBufReader = new BufferedReader(new InputStreamReader(templateResStream))) {
         String line = templateBufReader.readLine();
         while (line != null) {
            stringBuilder.append(line).append('\n');
            line = templateBufReader.readLine();
         }
      } finally {
         try {
            templateResStream.close();
         } catch (IOException e) {
            //nothing to do
         }
      }

      return stringBuilder.toString();
   }


   @Test
   public void testGenerate() throws IOException {
      UserMgmtServer userMgmtServer = TestUserMgmtServerValidService_Ldap.loadTestData("ldap-server.json");

      String actual = sssdConfigurationGenerator.getConfigurationContent(userMgmtServer, new String[]{"ldap-users"});

      System.out.println(actual);

      Assert.assertEquals(actual, readResource("sssd.conf.ldap137"));
   }

   @Test
   public void testGenerate1() throws IOException {
      UserMgmtServer userMgmtServer = TestUserMgmtServerValidService_Ldap.loadTestData("ad-server.json");

      String actual = sssdConfigurationGenerator.getConfigurationContent(userMgmtServer, new String[]{"Administrators", "Users"});

      System.out.println(actual);

      Assert.assertEquals(actual, readResource("sssd.conf.ldaps181"));
   }
}
