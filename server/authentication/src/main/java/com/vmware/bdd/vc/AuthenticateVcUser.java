/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.security.tls.TlsClientConfiguration;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.UserSession;
import com.vmware.vim.binding.vim.version.version8;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.impl.HttpConfigurationImpl;
import com.vmware.vim.vmomi.core.types.VmodlContext;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;

public class AuthenticateVcUser {
   private final static Logger LOGGER = Logger.getLogger(AuthenticateVcUser.class);
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
            //tempo solution, when connect to another VC, disable certificate verification.
            if(Configuration.getBoolean(Constants.CONNECT_TO_ANOTHER_VC, false)) {
               LOGGER.info("the BDE server is configured to connect to another vCenter, whose certificate will not be verified.");
               return Result.MATCH;
            }

            //default and good behavior.
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
      SessionManager sessionManager = null;
      try {

         URI uri = new URI(serviceUrl);
         HttpConfiguration httpConfig = new HttpConfigurationImpl();
         httpConfig.setThumbprintVerifier(getThumbprintVerifier());
         HttpClientConfiguration clientConfig =
               HttpClientConfiguration.Factory.newInstance();
         //set customized SSL protocols
         TlsClientConfiguration tlsClientConfiguration = new TlsClientConfiguration();
         httpConfig.setEnabledProtocols(tlsClientConfiguration.getSslProtocols());
         clientConfig.setHttpConfiguration(httpConfig);
         vmomiClient = Client.Factory.createClient(uri, version, clientConfig);

         ManagedObjectReference svcRef = new ManagedObjectReference();
         svcRef.setType("ServiceInstance");
         svcRef.setValue("ServiceInstance");

         if (CommonUtil.isBlank(name)) { // VC session token auth
            // use soap id to impersonate current VC user
            vmomiClient.getBinding().setSession(vmomiClient.getBinding().createSession(password));
         }

         ServiceInstance instance =
               vmomiClient.createStub(ServiceInstance.class, svcRef);
         ServiceInstanceContent instanceContent = instance.retrieveContent();
         sessionManager =
               vmomiClient.createStub(SessionManager.class,
                     instanceContent.getSessionManager());

         if (!CommonUtil.isBlank(name)) { // username/passowrd auth
            sessionManager.login(name, password, sessionManager.getDefaultLocale());
            sessionManager.logout();
         } else { // VC session token auth
            UserSession session = sessionManager.getCurrentSession();
            if (session == null) {
               throw new BadCredentialsException("invalid vc session.");
            } else {
               LOGGER.info(session.getUserName() + " is authenticated");
            }

          }
      } finally {
         if (vmomiClient != null) {
            vmomiClient.shutdown();
         }
      }
   }
}
