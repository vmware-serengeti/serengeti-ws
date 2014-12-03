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

import java.io.File;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.vmware.bdd.apitypes.Password;
import com.vmware.bdd.security.tls.SimpleServerTrustManager;
import com.vmware.bdd.security.tls.TlsTcpClient;

/**
 * Created By xiaoliangl on 12/2/14.
 */
@Configuration
@ImportResource(value = {"classpath:/com/vmware/bdd/usermgmt/userMgmtServerValidService-test-context.xml"})
public class TestUserMgmtServerValidServiceContext {
   public final static String KEY_STORE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "keystore.jks";

   @Bean
   public TlsTcpClient tlsTcpClient() {
      TlsTcpClient tlsTcpClient = new TlsTcpClient();

      SimpleServerTrustManager trustManager = new SimpleServerTrustManager();
      trustManager.setTrustStorePath(KEY_STORE_PATH);
      trustManager.setPassword(new Password() {
         @Override
         public String getPlainString() {
            return "changeit";
         }

         @Override
         public char[] getPlainChars() {
            return getPlainString().toCharArray();
         }
      });
      trustManager.setTrustStoreType("JKS");

      tlsTcpClient.setTrustManager(trustManager);

      return tlsTcpClient;
   }
}
