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

import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.config.CliProperties;
import com.vmware.bdd.cli.config.RunWayConfig;
import com.vmware.bdd.utils.ByteArrayUtils;
import com.vmware.bdd.utils.CommonUtil;
import jline.console.ConsoleReader;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xiaoliangl on 9/16/15.
 */
@Component
public class DefaultTrustManager implements X509TrustManager {
   private final static Logger logger = Logger.getLogger(DefaultTrustManager.class);
   private static final String KEY_STORE_FILE = "serengeti.keystore";

   private KeyStore keyStore;
   private static final char[] DEFAULT_PASSWORD = "changeit".toCharArray();

   @Autowired
   private CliProperties cliProperties;

   public DefaultTrustManager(){}


   @PostConstruct
   protected void initKeystore() throws KeyStoreException {
      this.keyStore = KeyStore.getInstance("jks");
   }

   @Override
   public void checkClientTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
   }

   @Override
   public void checkServerTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
      String errorMsg = "";
      InputStream in = null;
      OutputStream out = null;

      // load key store file
      try {
         char[] pwd = cliProperties.readKeyStorePwd();
         File file = new File(KEY_STORE_FILE);

         if (file.exists() && file.isFile()) {
            keyStore.load(new FileInputStream(file), pwd);
         } else {
            //init an empty keystore
            keyStore.load(null, pwd);
         }

         // show certificate informations
         MessageDigest sha1 = MessageDigest.getInstance("SHA1");
         MessageDigest md5 = MessageDigest.getInstance("MD5");
         String md5Fingerprint = "";
         String sha1Fingerprint = "";
         SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy/MM/dd");
         for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            sha1.update(cert.getEncoded());
            md5.update(cert.getEncoded());
            md5Fingerprint = ByteArrayUtils.byteArrayToHexString(md5.digest());
            sha1Fingerprint = ByteArrayUtils.byteArrayToHexString(sha1.digest());
            if (keyStore.getCertificate(md5Fingerprint) != null) {
               if (i == chain.length - 1) {
                  return;
               } else {
                  continue;
               }
            }
            System.out.println();
            System.out.println("Server Certificate");
            System.out
                  .println("================================================================");
            System.out.println("Subject:  " + cert.getSubjectDN());
            System.out.println("Issuer:  " + cert.getIssuerDN());
            System.out.println("SHA Fingerprint:  " + sha1Fingerprint);
            System.out.println("MD5 Fingerprint:  " + md5Fingerprint);
            System.out.println("Issued on:  "
                  + dateFormate.format(cert.getNotBefore()));
            System.out.println("Expires on:  "
                  + dateFormate.format(cert.getNotAfter()));
            System.out.println("Signature:  " + cert.getSignature());
            System.out.println();
            if (checkExpired(cert.getNotBefore(), cert.getNotAfter())) {
               throw new CertificateException(
                     "The security certificate has expired.");
            }
            ConsoleReader reader = new ConsoleReader();
            // Set prompt message
            reader.setPrompt(Constants.PARAM_PROMPT_ADD_CERTIFICATE_MESSAGE);
            // Read user input
            String readMsg;
            if (RunWayConfig.getRunType().equals(RunWayConfig.RunType.MANUAL)) {
               readMsg = reader.readLine().trim();
            } else {
               readMsg = "yes";
            }
            if ("yes".equalsIgnoreCase(readMsg) || "y".equalsIgnoreCase(readMsg)) {
               {
                  // add new certificate into key store file.
                  keyStore.setCertificateEntry(md5Fingerprint, cert);
                  out = new FileOutputStream(KEY_STORE_FILE);
                  keyStore.store(out, pwd);
                  CommonUtil.setOwnerOnlyReadWrite(KEY_STORE_FILE);
                  // save keystore password
                  cliProperties.saveKeyStorePwd(pwd);
               }
            } else {
               if (i == chain.length - 1) {
                  throw new CertificateException(
                        "Could not find a valid certificate in the keystore.");
               } else {
                  continue;
               }
            }
         }
      } catch (FileNotFoundException e) {
         errorMsg = "Cannot find the keystore file: " + e.getMessage();
      } catch (NoSuchAlgorithmException e) {
         errorMsg = "SSL Algorithm not supported: " + e.getMessage();
      } catch (IOException e) {
         e.printStackTrace();
         errorMsg = "IO error: " + e.getMessage();
      } catch (KeyStoreException e) {
         errorMsg = "Keystore error: " + e.getMessage();
      } catch (ConfigurationException e) {
         errorMsg = "cli.properties access error: " + e.getMessage();
      } finally {
         if (!CommandsUtils.isBlank(errorMsg)) {
            System.out.println(errorMsg);
            logger.error(errorMsg);
         }
         if (in != null) {
            try {
               in.close();
            } catch (IOException e) {
               logger.warn("Input stream of serengeti.keystore close failed.");
            }
         }
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               logger.warn("Output stream of serengeti.keystore close failed.");
            }
         }
      }
   }

   private boolean checkExpired(Date notBefore, Date notAfter) {
      Date now = new Date();
      if (now.before(notBefore) || now.after(notAfter)) {
         return true;
      }
      return false;
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return null;
   }


}
