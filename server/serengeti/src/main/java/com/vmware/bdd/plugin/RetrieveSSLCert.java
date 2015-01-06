/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 ***************************************************************************/
package com.vmware.bdd.plugin;


import java.security.MessageDigest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class RetrieveSSLCert {
   public static String retrieveSSLCert(String host, int port) throws Exception {
      try {
         javax.net.ssl.TrustManager[] trustAllCerts =
               new javax.net.ssl.TrustManager[1];
         javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
         trustAllCerts[0] = tm;

         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init(null, trustAllCerts, null);

         SSLSocketFactory factory = sc.getSocketFactory();
         SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
         socket.startHandshake();
         SSLSession session = socket.getSession();
         java.security.cert.Certificate[] servercerts =
               session.getPeerCertificates();

         MessageDigest md = MessageDigest.getInstance("SHA-1");

         String thumbPrint = "";
         if (servercerts != null && servercerts.length > 0) {
            java.security.cert.Certificate cert = servercerts[0];
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            thumbPrint = hexify(digest);
         }

         socket.close();
         return thumbPrint;
      } catch (Exception e) {
         return "";
      }
   }

   private static String hexify(byte bytes[]) {
      char[] hexDigits =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                  'd', 'e', 'f' };

      char separator = ':';

      StringBuffer buf = new StringBuffer(bytes.length * 2);

      int length = bytes.length;
      for (int i = 0; i < length; ++i) {
         buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
         buf.append(hexDigits[bytes[i] & 0x0f]);
         if (i != length - 1) {
            buf.append(separator);
         }
      }

      return buf.toString();
   }
}
