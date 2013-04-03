/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.security.sso;

import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.vmware.vim.sso.client.DefaultSecurityTokenServiceFactory;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.SecurityTokenService;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig.ConnectionConfig;
import com.vmware.vim.sso.client.TokenSpec;


public class TestAccountService {

   //@Test
   public void testLoadCertificate() throws Exception {
      URL stsURL = new URL("https://10.110.170.6:7444/ims/STSService?wsdl");
      FileInputStream bis = new FileInputStream("c:\\bdc\\sdk\\sts.crt");
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      List<X509Certificate> stsCerts = new ArrayList<X509Certificate>();
      while (bis.available() > 0) {
         stsCerts.add((X509Certificate)cf.generateCertificate(bis));
      }
      X509Certificate[] certs = stsCerts.toArray(new X509Certificate[stsCerts.size()]);
      ConnectionConfig connConfig = new ConnectionConfig(stsURL, certs, null);
      SecurityTokenServiceConfig config = new SecurityTokenServiceConfig(connConfig, connConfig.getTrustedRootCertificates(), null);

      // Create STS client
      SecurityTokenService stsClient = DefaultSecurityTokenServiceFactory.getSecurityTokenService(config);


      // Describe the requested token properties using a TokenSpec
      TokenSpec tokenSpec = new TokenSpec.Builder(60).createTokenSpec();

      // Acquire the requested token
      SamlToken token = stsClient.acquireToken("lzhai@aurora.dev", "Password_1", tokenSpec);
      System.out.println(token.toXml());
   }
}
