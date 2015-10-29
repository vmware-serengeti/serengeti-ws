/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.vim.Extension;
import com.vmware.vim.binding.vim.ExtensionManager;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.fault.InvalidLocale;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.binding.vim.fault.NotFound;

public abstract class NgcRegistrar {

   private static final Logger LOGGER = Logger.getLogger(NgcRegistrar.class);
   protected VcService vcService;
   protected ExtensionManager extensionManager;

   protected abstract Extension generateNgcExtension();

   protected String getVmIpAddress() throws SocketException {
      // NOTE: we will only use eth0 as the external network interface
      String validInterface = "eth0";

      Enumeration<NetworkInterface> interfaces =
            NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
         NetworkInterface in = interfaces.nextElement();
         if (in.getName().equals(validInterface)) {
            Enumeration<InetAddress> addresses = in.getInetAddresses();
            while (addresses.hasMoreElements()) {
               InetAddress inetAddress = addresses.nextElement();
               if ((inetAddress instanceof Inet4Address)
                     && !inetAddress.isLoopbackAddress()) {
                  return inetAddress.getHostAddress();
               }
            }
         }
      }
      return null;
   }

   protected void registerExtension() {
      Extension ngcExtension = generateNgcExtension();
      LOGGER.info("Trying to install the plugin..." + ngcExtension.getKey());
      extensionManager.registerExtension(ngcExtension);
      LOGGER.info(ngcExtension.getKey() + " plugin installation succeeded!");
   }

   /**
    * The unregister NGC plugin process is as the following -- 1. impersonate
    * from current management server extension to UI extension; 2. delete the
    * current UI extension; 3. impersonate back as the management server
    * extension;
    * 
    * This method is fine because no other process will use VC context after it
    * is initialized, and the NGC unregistration only happens during the boot of
    * management server (i.e., management server will not accept any request
    * until it is booted up, and thus this is safe here)
    * 
    * @throws VcException
    * @throws InvalidLocale
    * @throws InvalidLogin
    */
   protected void unregisterExtension(String ngcKey) throws InvalidLogin,
         InvalidLocale, NotFound {
      SessionManager sessionManager = null;
      try {
         LOGGER.info("Trying to uninstall the plugin..." + ngcKey);

         sessionManager = vcService.getSessionManager();
         sessionManager.impersonateUser(ngcKey, vcService.getLocale());

         extensionManager.unregisterExtension(ngcKey);
         LOGGER.info(ngcKey + " plugin uninstallation succeeded!");
      } finally {
         if (sessionManager != null) {
            try {
               sessionManager.impersonateUser(vcService.getExtensionKey(),
                     vcService.getLocale());
            } catch (InvalidLogin | InvalidLocale ex) {
               LOGGER.error("Error to impersonate user.");
            }
         }
      }
   }

   protected void updateExtension() throws CertificateEncodingException,
         KeyStoreException, NoSuchAlgorithmException, SocketException,
         NotFound, InvalidLogin, InvalidLocale {

      SessionManager sessionManager = null;
      try {
         Extension ngcExtension = generateNgcExtension();
         LOGGER.info("Trying to update the old plugin..."
               + ngcExtension.getKey());

         sessionManager = vcService.getSessionManager();
         sessionManager.impersonateUser(ngcExtension.getKey(),
               vcService.getLocale());
         extensionManager.updateExtension(ngcExtension);
         LOGGER.info(ngcExtension.getKey() + " plugin updation succeeded!");
      } finally {
         if (sessionManager != null) {
            try {
               sessionManager.impersonateUser(vcService.getExtensionKey(),
                     vcService.getLocale());
            } catch (InvalidLogin | InvalidLocale ex) {
               LOGGER.error("Error to impersonate user.");
            }
         }
      }

   }

}
