/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.aurora.util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;

import com.vmware.aurora.security.ThumbprintTrustManager;

public class HttpsConnectionUtil {
   private static final Logger logger = Logger.getLogger(HttpsConnectionUtil.class);

   private static HostnameVerifier hnv;
   private static SSLContext sc;
   private static ThumbprintTrustManager tm = null;

   static {
      // Setup SSL settings for the standard HTTPS handler
      hnv = new HostnameVerifier() {
         @Override
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };
      try {
         sc = SSLContext.getInstance("TLS");
      } catch (Exception e) {
         logger.error("Cannot find SSL instance", e);
      }
   }

   static synchronized public void init(String thumbprint) {
      if (tm != null) {
         return;
      }
      logger.debug("Disabling host verification for default URL connections");
      try {
         HttpsURLConnection.setDefaultHostnameVerifier(hnv);
      } catch (Exception e) {
         logger.error("Failed to disable host verification", e);
      }

      // Install an SSL trust manager for outgoing SSL connections
      // This code assumes that we only do outgoing connections to the vCenter
      // server.
      logger.debug("Installing thumbprint SSL verification");
      try {
         tm = new ThumbprintTrustManager();
         tm.add(thumbprint);
//         sc.init(null, new TrustManager[]{tm}, new java.security.SecureRandom());
//         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//         SSLContext.setDefault(sc);
      } catch (Exception e) {
         logger.error("Failed setup SSL trust manager", e);
      }
   }
   
   static public ThumbprintTrustManager getThumbprintTrustManager() {
      return tm;
   }
}
