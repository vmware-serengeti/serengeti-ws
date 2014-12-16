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
package com.vmware.aurora.vc.vcservice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

/**
 * Created By xiaoliangl on 12/16/14.
 */
public class TlsSocketFactory implements SecureProtocolSocketFactory {
   /**
    * Log object for this class.
    */
   private static final Logger LOG = Logger.getLogger(TlsSocketFactory.class);

   private SSLContext sslcontext = null;

   private TrustManager[] trustManagers = null;

   public TlsSocketFactory(TrustManager[] trustManagers1) {
      trustManagers = trustManagers1;
   }

   private SSLContext createEasySSLContext() {
      try {
         SSLContext context = SSLContext.getInstance("TLS");
         context.init(
               null,
               trustManagers,
               null);
         return context;
      } catch (Exception e) {
         LOG.error(e.getMessage(), e);
         throw new HttpClientError(e.toString());
      }
   }

   private SSLContext getSSLContext() {
      if (this.sslcontext == null) {
         this.sslcontext = createEasySSLContext();
      }
      return this.sslcontext;
   }

   @Override
   public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
      return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
      );
   }

   @Override
   public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
      return getSSLContext().getSocketFactory().createSocket(
            host,
            port,
            localAddress,
            localPort
      );
   }

   @Override
   public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
      if (params == null) {
         throw new IllegalArgumentException("Parameters may not be null");
      }
      int timeout = params.getConnectionTimeout();
      SocketFactory socketfactory = getSSLContext().getSocketFactory();
      if (timeout == 0) {
         return socketfactory.createSocket(host, port, localAddress, localPort);
      } else {
         Socket socket = socketfactory.createSocket();
         socket.bind(new InetSocketAddress(localAddress, localPort));
         socket.connect(new InetSocketAddress(host, port), timeout);
         return socket;
      }
   }

   @Override
   public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      return getSSLContext().getSocketFactory().createSocket(
            host,
            port
      );
   }
}
