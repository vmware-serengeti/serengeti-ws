/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;

public class InstallCert {

   public static void main(String[] args) throws Exception {
      String host = "127.0.0.1";
      int port = 8443;
      char[] passphrase = "changeit".toCharArray();
      String keyStorePath = "/opt/serengeti/cli/serengeti.keystore";
      File file = new File(keyStorePath);
      if (file.isFile() == false) {
         char SEP = File.separatorChar;
         File dir =
               new File(System.getProperty("java.home") + SEP + "lib" + SEP
                     + "security");
         file = new File(dir, "serengeti.keystore");
         if (file.isFile() == false) {
            file = new File(dir, "cacerts");
         }
      }
      InputStream in = new FileInputStream(file);
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      ks.load(in, passphrase);
      in.close();
      SSLContext context = SSLContext.getInstance("TLS");
      TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory
                  .getDefaultAlgorithm());
      tmf.init(ks);
      X509TrustManager defaultTrustManager =
            (X509TrustManager) tmf.getTrustManagers()[0];
      SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
      context.init(null, new TrustManager[] { tm }, null);
      SSLSocketFactory factory = context.getSocketFactory();
      SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
      socket.setSoTimeout(10000);
      try {
         System.out.println("Starting SSL handshake...");
         socket.startHandshake();
         socket.close();
      } catch (SSLException e) {
         System.out.println("The certificate has not been trusted");
      }
      X509Certificate[] chain = tm.chain;
      if (chain == null) {
         System.out.println("Could not obtain server certificate chain");
         return;
      }
      X509Certificate cert = chain[0];
      ks.setCertificateEntry(String.valueOf(cert.hashCode()), cert);
      OutputStream out = new FileOutputStream(keyStorePath);
      ks.store(out, passphrase);
      out.close();
      System.out
            .println("Added certificate to keystore 'serengeti.keystore' using alias '"
                  + String.valueOf(cert.hashCode()) + "'");
   }

   private static class SavingTrustManager implements X509TrustManager {

      private final X509TrustManager tm;
      private X509Certificate[] chain;

      SavingTrustManager(X509TrustManager tm) {
         this.tm = tm;
      }

      public X509Certificate[] getAcceptedIssuers() {
         throw new UnsupportedOperationException();
      }

      public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
         throw new UnsupportedOperationException();
      }

      public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
         this.chain = chain;
         tm.checkServerTrusted(chain, authType);
      }
   }

}