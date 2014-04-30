/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.vc;

import java.net.URI;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.version.version8;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.impl.HttpConfigurationImpl;
import com.vmware.vim.vmomi.core.types.VmodlContext;

public class AuthenticateVcUser {
   private static final Class<?> version = version8.class;
   private String vcThumbprint;
   private String serviceUrl;

   static {
      try {
         VmodlContext.getContext();
      } catch (IllegalStateException ex) {
         VmodlContext
               .initContext(new String[] { "com.vmware.vim.binding.vim" });
      }
   }

   public AuthenticateVcUser(String vcHost, int vcPort) {
      this(vcHost, vcPort, null);
   }

   public AuthenticateVcUser(String vcHost, int vcPort, String vcThumbprint) {
      serviceUrl = "https://" + vcHost + ":" + vcPort + "/sdk";
      this.vcThumbprint = vcThumbprint;
   }

   private ThumbprintVerifier getThumbprintVerifier() {
      return new ThumbprintVerifier() {
         @Override
         public Result verify(String thumbprint) {
            if (vcThumbprint == null
                  || vcThumbprint.equalsIgnoreCase(thumbprint)) {
               return Result.MATCH;
            } else {
               return Result.MISMATCH;
            }
         }

         @Override
         public void onSuccess(X509Certificate[] chain, String thumbprint,
               Result verifyResult, boolean trustedChain,
               boolean verifiedAssertions) throws SSLException {
         }
      };
   }

   public void authenticateUser(String name, String password) throws Exception {
      Client vmomiClient = null;
      try {
         URI uri = new URI(serviceUrl);
         HttpConfiguration httpConfig = new HttpConfigurationImpl();
         httpConfig.setThumbprintVerifier(getThumbprintVerifier());
         HttpClientConfiguration clientConfig =
               HttpClientConfiguration.Factory.newInstance();
         clientConfig.setHttpConfiguration(httpConfig);
         vmomiClient = Client.Factory.createClient(uri, version, clientConfig);

         ManagedObjectReference svcRef = new ManagedObjectReference();
         svcRef.setType("ServiceInstance");
         svcRef.setValue("ServiceInstance");

         ServiceInstance instance =
               vmomiClient.createStub(ServiceInstance.class, svcRef);
         ServiceInstanceContent instanceContent = instance.retrieveContent();
         SessionManager sessionManager =
               vmomiClient.createStub(SessionManager.class,
                     instanceContent.getSessionManager());

         sessionManager.login(name, password, sessionManager.getDefaultLocale());
         sessionManager.logout();
      } finally {
         if (vmomiClient != null) {
            vmomiClient.shutdown();
         }
      }
   }
}