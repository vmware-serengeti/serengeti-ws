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
package com.vmware.bdd.security.tls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.lang.ArrayUtils;

import com.vmware.bdd.utils.CommonUtil;

/**
 * this ssl socket factory is integrated with SimpleServerTrustFactory for graceful server certificate validation.
 */
public class SimpleSeverTrustTlsSocketFactory extends SSLSocketFactory {

   private final SSLSocketFactory defaultSSLSocketFactory;
   private SSLParameters sslParams;

   /**
    * Wrap a socket to enable custom configuration(ciphers and protocols) to be
    * supported for the connection
    *
    * @param sock a socket created by the
    *             {@link SSLSocketFactory#createSocket() method}
    * @return a wrapped socket which has the client specified configuration
    */
   private Socket wrapSocket(Socket sock) {
      SSLSocket sslSock = (SSLSocket) sock;
      sslSock.setSSLParameters(sslParams);
      try {
         sslSock.setSoTimeout(30000);
      } catch (SocketException e) {
         //
      }
      return sslSock;
   }

   /**
    * Wrap an existing factory
    *
    * @param factory   the SSLSocketFactory as returned by
    *                  {@link javax.net.ssl.SSLContext#getSocketFactory()}
    * @param sslParams The configuration to be set on the SSLSocket that is
    *                  created by the socket factory
    */
   public SimpleSeverTrustTlsSocketFactory(SSLSocketFactory factory,
                                           final SSLParameters sslParams) {
      this.defaultSSLSocketFactory = factory;
      this.sslParams = sslParams;
   }

   @Override
   public Socket createSocket(Socket s, String host, int port,
                              boolean autoClose) throws IOException {
      return wrapSocket(defaultSSLSocketFactory.createSocket(s, host, port,
            autoClose));
   }

   @Override
   public Socket createSocket(String host, int port) throws IOException,
         UnknownHostException {
      return wrapSocket(defaultSSLSocketFactory.createSocket(host, port));
   }

   @Override
   public Socket createSocket(InetAddress host, int port) throws IOException {
      return wrapSocket(defaultSSLSocketFactory.createSocket(host, port));
   }

   @Override
   public Socket createSocket(String host, int port, InetAddress localHost,
                              int localPort) throws IOException, UnknownHostException {
      return wrapSocket(defaultSSLSocketFactory.createSocket(host, port,
            localHost, localPort));
   }

   @Override
   public Socket createSocket(InetAddress address, int port,
                              InetAddress localAddress, int localPort) throws IOException {
      return wrapSocket(defaultSSLSocketFactory.createSocket(address, port,
            localAddress, localPort));
   }

   @Override
   public String[] getDefaultCipherSuites() {
      if (this.sslParams.getCipherSuites() != null) {
         return this.sslParams.getCipherSuites();
      }
      return defaultSSLSocketFactory.getDefaultCipherSuites();
   }

   @Override
   public String[] getSupportedCipherSuites() {
      return defaultSSLSocketFactory.getSupportedCipherSuites();
   }

   @Override
   public Socket createSocket() throws IOException {
      return wrapSocket(defaultSSLSocketFactory.createSocket());
   }

   /**
    * init(..) before call this method
    *
    * @return the default factory.
    */
   public static SocketFactory getDefault() {
      return makeSSLSocketFactory(trustStoreConfig);
   }

   private static TrustStoreConfig trustStoreConfig = null;

   /**
    * init required parameters for getDefault()
    */
   public static void init(TrustStoreConfig trustStoreConfig1) {
      trustStoreConfig = trustStoreConfig1;
   }


   private static void check(TrustStoreConfig trustStoreCfg) {
      if (trustStoreCfg == null) {
         throw new TlsInitException("SIMPLE_TLS_SOCK_FACTORY.PARAMS_REQUIRED", null, "trust store config object.");
      }

      if(trustStoreCfg.getPassword() == null) {
         throw new TlsInitException("SIMPLE_TLS_SOCK_FACTORY.PARAMS_REQUIRED", null, "PasswordProvider");
      } else if(ArrayUtils.isEmpty(trustStoreCfg.getPassword().getPlainChars())) {
         throw new TlsInitException("SIMPLE_TLS_SOCK_FACTORY.PARAMS_REQUIRED", null, "Password");
      }

      if(CommonUtil.isBlank(trustStoreCfg.getPath())) {
         throw new TlsInitException("SIMPLE_TLS_SOCK_FACTORY.PARAMS_REQUIRED", null, "Trust Store Path");
      }

      if(CommonUtil.isBlank(trustStoreCfg.getType())) {
         throw new TlsInitException("SIMPLE_TLS_SOCK_FACTORY.PARAMS_REQUIRED", null, "Trust Store Type");
      }
   }

   /**
    * factory method for custom usage.
    *
    * @return a factory
    */
   public static SSLSocketFactory makeSSLSocketFactory(TrustStoreConfig trustStoreCfg) {
      check(trustStoreCfg);


      SimpleServerTrustManager simpleServerTrustManager = new SimpleServerTrustManager();
      simpleServerTrustManager.setTrustStoreConfig(trustStoreCfg);
      /**
       *  Initialize our own trust manager
       */
      TrustManager[] trustManagers = new TrustManager[]{simpleServerTrustManager};

      SSLContext customSSLContext = null;
      try {
         /**
          * Instantiate a context that implements the family of TLS protocols
          */
         customSSLContext = SSLContext.getInstance("TLS");

         /**
          * Initialize SSL context. Default instances of KeyManager and
          * SecureRandom are used.
          */
         customSSLContext.init(null, trustManagers, null);
      } catch (NoSuchAlgorithmException e) {
         throw new TlsInitException("SSLContext_INIT_ERR", e);
      } catch (KeyManagementException e) {
         throw new TlsInitException("SSLContext_INIT_ERR", e);
      }

      TlsClientConfiguration tlsClientConfiguration = new TlsClientConfiguration();
      /**
       * Build connection configuration and pass to socket
       */
      SSLParameters params = new SSLParameters();
      params.setCipherSuites(tlsClientConfiguration.getCipherSuites());
      params.setProtocols(tlsClientConfiguration.getSslProtocols());
//      params.setEndpointIdentificationAlgorithm(
//            config.getEndpointIdentificationAlgorithm());
      /**
       * Use the SSLSocketFactory generated by the SSLContext and wrap it to
       * enable custom cipher suites and protocols
       */
      return new SimpleSeverTrustTlsSocketFactory(customSSLContext.getSocketFactory(), params);
   }
}



