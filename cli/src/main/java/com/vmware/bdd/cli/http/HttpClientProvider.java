/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.cli.http;

import com.vmware.bdd.cli.config.CliProperties;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by xiaoliangl on 9/16/15.
 */
@Configuration
public class HttpClientProvider {
   private final static Logger LOGGER = Logger.getLogger(HttpClientProvider.class);

   public static final String SECURE_HTTP_CLIENT = "secureHttpClient";

   @Autowired
   private X509TrustManager trustManager;

   @Autowired
   private CliProperties cliProperties;

   @Autowired
   private HostnameVerifiers hostnameVerifiers;

   @Bean(name = SECURE_HTTP_CLIENT)
   @Qualifier(SECURE_HTTP_CLIENT)
   public HttpClient secureHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
      SSLContext sslContext = SSLContexts.custom().useTLS().build();


      sslContext.init(null, new TrustManager[]{trustManager}, null);

      String[] supportedProtocols = cliProperties.getSupportedProtocols();
      String[] supportedCipherSuites = cliProperties.getSupportedCipherSuites();
      String hostnameVerifier = cliProperties.getHostnameVerifier();

      if(LOGGER.isDebugEnabled()) {
         LOGGER.debug("supported protocols: " + ArrayUtils.toString(supportedProtocols));
         LOGGER.debug("supported cipher suites: " + ArrayUtils.toString(supportedCipherSuites));
         LOGGER.debug("hostname verifier: " + hostnameVerifier);
      }

      PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
      cm.setMaxTotal(20);
      cm.setDefaultMaxPerRoute(10);

      SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
            sslContext, supportedProtocols,supportedCipherSuites,
            getHostnameVerifier(hostnameVerifier));

//      HttpHost proxy = new HttpHost("127.0.0.1", 8810, "http");
//      HttpClient  client1 = HttpClients.custom().setSSLSocketFactory(socketFactory).setProxy(proxy).build();

      HttpClient client1 = HttpClients.custom().setSSLSocketFactory(socketFactory).setConnectionManager(cm).build();
      return client1;
   }

   private X509HostnameVerifier getHostnameVerifier(String verifier) {
      return hostnameVerifiers.getHostnameVerifier(verifier);
   }
}
