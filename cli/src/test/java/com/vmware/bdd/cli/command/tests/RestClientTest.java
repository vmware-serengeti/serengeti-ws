/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.vmware.bdd.cli.commands.ClusterCommands;
import com.vmware.bdd.cli.rest.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

import com.vmware.bdd.cli.commands.Constants;

@Test
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class RestClientTest extends AbstractTestNGSpringContextTests {
   private String cli_property_file = "cli_test.properties";

   @Autowired
   private RestClient client;

   @Test
   public void testGetHostUriProperty() {
      String host ="serengeti.com:9090";
      FileOutputStream hostFile = null;
      try {
         Properties hostProperty = new Properties();
         hostProperty.setProperty(Constants.PROPERTY_HOST, host);
         hostFile = new FileOutputStream(cli_property_file);
         hostProperty.store(hostFile, Constants.PROPERTY_FILE_HOST_COMMENT);
      } catch (IOException e) {
         System.out.println(Constants.PROPERTY_FILE_HOST_FAILURE);
      } finally {
         if (hostFile != null) {
            try {
               hostFile.close();
            } catch (IOException e) {
               //nothing to do
            }
         }
      }
      String hostUri = getHostUriProperty();
      assertEquals(hostUri, "https://serengeti.com:9090/serengeti/api/");
      delete(cli_property_file);
   }
   
   private void delete(String fileName) {
      File file = new File(fileName);
      
      if (file.exists()) {
         file.delete();
      }
   }

   /*
    *  Get Serengeti host from cli property file
    */
   private String getHostUriProperty() {
      String hostUri = null;
      FileReader hostFileReader = null;
      
      try {
         hostFileReader = new FileReader(cli_property_file);
         Properties hostProperties = new Properties();
         hostProperties.load(hostFileReader);
         
         if (hostProperties != null && hostProperties.get(Constants.PROPERTY_HOST) != null) {
            hostUri = Constants.HTTPS_CONNECTION_PREFIX + (String) hostProperties.get(Constants.PROPERTY_HOST)
                  + Constants.HTTPS_CONNECTION_SUFFIX;
         }
      } catch (Exception e) {//not set yet; or read io error
         //nothing to do
      } finally {
         if (hostFileReader != null) {
            try {
               hostFileReader.close();
            } catch (IOException e) {
               //nothing to do
            }
         }
      }
      
      return hostUri;
   }
}
