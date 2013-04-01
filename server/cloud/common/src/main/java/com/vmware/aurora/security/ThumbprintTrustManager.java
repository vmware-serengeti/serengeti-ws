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
package com.vmware.aurora.security;

import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

/**
 * Our custom trust manager is instantiated with expected thumbprints. Instead
 * of checking that the server certificate is trusted (which is typically not
 * the case, because providers have generated self-signed certificates), we
 * check that the thumbprint of the certificate is as expected.
 */
public class ThumbprintTrustManager implements X509TrustManager {
   private static final Logger logger = Logger.getLogger(ThumbprintTrustManager.class);
   private MessageDigest sha1;
   private Map<String, ThumbprintHolder> thumbprints;
   
   static class ThumbprintHolder {
      List<Object> owners = null;       // keep track of owners
      boolean permanent;                // always held in thumbprint Hashtable
      
      private void addOwner(Object owner) {
         if (permanent) {
            return;
         }
         if (owner == null) {
            // promote holder to permanent
            permanent = true;
            owners = null;
         } else {
            owners.add(owner);
         }
      }
      
      private void removeOwner(Object owner) {
         if (permanent) {
            return;
         }
         if (owners != null) {
            owners.remove(owner);
         }
      }

      public ThumbprintHolder(Object owner) {
         permanent = (owner == null);
         if (!permanent) {
            owners = new ArrayList<Object>();
            owners.add(owner);
         }
      }
      
      private boolean canRemove() {
         return !permanent && owners.isEmpty();
      }
   }
 
   /*
    * Add a thumbprint owned by "owner".
    * If owner is null, keep the thumbprint until it is removed by force.
    */
   public synchronized void add(String thumbprint, Object owner) {
      String tp = thumbprint.toLowerCase();
      ThumbprintHolder holder = thumbprints.get(tp);
      if (holder == null) {
         holder = new ThumbprintHolder(owner);
         thumbprints.put(tp, holder);
      } else {
         holder.addOwner(owner);
      }
   }
   
   public void add(String thumbprint) {
      add(thumbprint, null);
   }
  
   /*
    * Remove a thumbprint owned by "owner".
    * If owner is null, force remove the thumbprint.
    */
   public synchronized void remove(String thumbprint, Object owner) {
      String tp = thumbprint.toLowerCase();
      ThumbprintHolder holder = thumbprints.get(tp);
      if (holder == null) {
         return;
      }
      holder.removeOwner(owner);
      if (owner == null || holder.canRemove()) {
         thumbprints.remove(tp);
      }
   }
   
   public void remove(String thumbprint) {
      remove(thumbprint, null);
   }
   
   public synchronized boolean hasThumbprint(String thumbprint) {
      String tp = thumbprint.toLowerCase();
      return thumbprints.get(tp) != null;
   }
   
   public ThumbprintTrustManager() throws Exception {
      thumbprints = new HashMap<String, ThumbprintHolder>();
      sha1 = MessageDigest.getInstance("SHA-1");
   }

   @Override
   public void checkClientTrusted(X509Certificate[] chain, String authType)
   throws CertificateException {
      throw new CertificateException();
   }

   /**
    * Encodes an array of bytes as hex symbols.
    *
    * @param bytes the array of bytes to encode
    * @param separator the separator to use between two bytes, can be null
    * @return the resulting hex string
    */
   public static String toHex(byte[] bytes, String separator) {
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < bytes.length; i++) {
         int unsignedByte = bytes[i] & 0xff;
         if (unsignedByte < 16) {
            result.append("0");
         }
         result.append(Integer.toHexString(unsignedByte));
         if (separator != null && i + 1 < bytes.length) {
            result.append(separator);
         }
      }
      return result.toString();
   }

   public static String toHex(byte[] bytes) {
      return toHex(bytes, null);
   }

   /**
    * Generates a thumbprint for a certificate
    * 
    * @param cert the certificate to generate a thumbprint for
    * @return the certificate thumbprint
    * @throws CertificateEncodingException
    */
   public String certificateToThumbprint(Certificate cert)
   throws CertificateEncodingException {
      sha1.reset();
      try {
         return toHex(sha1.digest(cert.getEncoded()), ":");
      } catch (CertificateEncodingException e) {
         logger.error(e);
         throw e;
      }
   }

   @Override
   public void checkServerTrusted(X509Certificate[] chain, String authType)
   throws CertificateException {

      if (chain.length == 0) {
         logger.warn("No certificates in chain");
         throw new CertificateException();
      }

      /*
       * Usually, there will be only one, self-signed certificate in the list.
       * But in the general case, we want to examine the first.
       * 
       * The TLS specification states that: "The sender's certificate must come
       * first in the list. Each following certificate must directly certify the
       * one preceding it."
       */
      String tp = certificateToThumbprint(chain[0]);
      if (!hasThumbprint(tp)) {
         logger.warn("Invalid SSL thumbprint received: " + tp);
         throw new CertificateException();
      }
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }
}
