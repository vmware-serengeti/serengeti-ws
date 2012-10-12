/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.serengeti;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class VcExtension {

   private static final Logger logger = Logger.getLogger(VcExtension.class);

   public class Settings {

      @Parameter(names = "-evsURL", description = "vCenter extension service URL", required = true)
      private String evsURL;

      @Parameter(names = "-evsToken", description = "Token for this serengeti vApp", required = true)
      private String evsToken;

      @Parameter(names = "-evsThumbprint", description = "vCenter X509 thumbprint", required = true)
      private String evsThumbprint;

      @Parameter(names = "-extKey", description = "Extension key used to register with vCenter", required = true)
      private String extKey;

      @Parameter(names = "-cert", description = "Certificate file", required = true)
      private String cert;
   }

   private Settings settings;

   private String GetCertificate() {
      String cert = null;
      try {
         BufferedReader br = new BufferedReader(new FileReader(settings.cert));
         try {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
               sb.append(line);
               sb.append("\n");
            }
            cert = sb.toString();
         } finally {
            br.close();
         }
      } catch (Exception e) {
         logger.error("Failed to load certificate:" + settings.cert, e);
      }

      return cert;
   }

   private synchronized void registerExtensionVService() {

      String cert = GetCertificate();

      if (cert == null)
         return;

      logger.debug("Registering extension vService at: " + settings.evsURL
            + " ExtensionKey: " + settings.extKey);
      try {

         ThumbprintTrustManager tm = new ThumbprintTrustManager();
         tm.add(settings.evsThumbprint);

         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init(null, new TrustManager[] { tm },
               new java.security.SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

         // Create all-trusting host name verifier
         HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
               return true;
            }
         };

         // Install the all-trusting host verifier
         HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

         URL url = new URL(settings.evsURL);
         URLConnection connection = url.openConnection();

         connection.setRequestProperty("evs-token", settings.evsToken);
         connection.setDoInput(true);
         connection.setDoOutput(true); // POST
         connection.setUseCaches(false);

         Writer output = new OutputStreamWriter(connection.getOutputStream());
         String evsSchema = "http://www.vmware.com/schema/vservice/ExtensionVService";
         String payload = "<RegisterExtension xmlns=\"" + evsSchema + "\">\n"
               + "  <Key>" + settings.extKey + "</Key>\n" + "  <Certificate>\n"
               + cert + "\n" + "  </Certificate>\n" + "</RegisterExtension>\n";
         output.write(payload);
         output.flush();
         connection.connect();

         // Read response headers
         Map<String, List<String>> headers = connection.getHeaderFields();
         for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (String val : e.getValue()) {
               logger.info("Response Header: " + e.getKey() + " :" + val);
            }
         }
         // Read response
         BufferedReader input = new BufferedReader(new InputStreamReader(
               connection.getInputStream()));
         for (String str = input.readLine(); str != null; str = input
               .readLine()) {
            logger.debug("Response: " + str);
         }
         input.close();
         output.close();

         logger.debug("Successfully registered");
      } catch (Exception e) {
         logger.error("Failed Extension registration to " + settings.evsURL, e);
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      VcExtension ext = new VcExtension();

      ext.settings = ext.new Settings();
      new JCommander(ext.settings, args);

      ext.registerExtensionVService();
   }

}
