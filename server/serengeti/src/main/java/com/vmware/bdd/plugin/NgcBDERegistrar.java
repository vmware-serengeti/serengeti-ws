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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.zip.ZipException;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.security.CmsKeyStore;
import com.vmware.aurora.security.JksKeyStoreUtil;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.impl.vim.DescriptionImpl;
import com.vmware.vim.binding.impl.vim.ExtensionImpl;
import com.vmware.vim.binding.impl.vim.ExtensionImpl.ClientInfoImpl;
import com.vmware.vim.binding.impl.vim.ExtensionImpl.ServerInfoImpl;
import com.vmware.vim.binding.vim.Description;
import com.vmware.vim.binding.vim.Extension;
import com.vmware.vim.binding.vim.Extension.ClientInfo;
import com.vmware.vim.binding.vim.Extension.ServerInfo;

/**
 * Register the ngc plugin as an extension service to the VC the oms server is
 * deployed to. If there is an existing version of the plugin registered, it
 * will overwrite that with the one from this server.
 */
public class NgcBDERegistrar extends NgcRegistrar {

   private static final Logger LOGGER = Logger.getLogger(NgcBDERegistrar.class);
   
   private String packageName = NgcConstants.PLUGIN_ZIP_NAME;

   public void initNgcRegistration() {
	  String vcVersion = VcContext.getVcVersion();
	  LOGGER.info("Get VC version:"+vcVersion);
      if("5.1.0".equals(vcVersion)) {
       	packageName = NgcConstants.PLUGIN_ZIP_NAME + "-5.1.0";
      }
      LOGGER.info("Packaging NGC BDE plugin tarball...");
      try{
    	  packageNgcTarball();
      } catch (IOException ex) {
    	  LOGGER.error("Packaging NGC BDE plugin tarball fails due to "+ ex.getMessage());
      }
      LOGGER.info("Starting to register NGC BDE plugin...");
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            vcService = VcContext.getService();
            extensionManager = vcService.getExtensionManager();
            LOGGER.info("get extensionManager...");
            Extension extension =
                  extensionManager.findExtension(NgcConstants.NGC_KEY);
            LOGGER.info("get extension..." + NgcConstants.NGC_KEY);

            if (extension == null) {
               LOGGER.info("register extension...");
               registerExtension();
            } else {
               LOGGER.info("update extension...");
               updateExtension();
            }
            LOGGER.info("NGC BDE plugin auto-installation succeeded!");
            return null;
         }

      });
   }

   private static String LOCAL_HOST = "localhost";
   private static int PORT_NUM = 443;

   @Override
   protected Extension generateNgcExtension() {
      try {
         String pluginUrl =
               NgcConstants.NGC_PLUGIN_URL_PREFIX + getVmIpAddress()
                     + NgcConstants.NGC_PLUGIN_URL_SUFFIX + packageName + ".zip";
         Extension extension = new ExtensionImpl();
         extension.setKey(NgcConstants.NGC_KEY);
         extension.setVersion(NgcConstants.NGC_VERSION);
         extension.setCompany(NgcConstants.NGC_COMPANY);


         Description description = new DescriptionImpl();
         description.setLabel(NgcConstants.NGC_LABEL);
         description.setSummary(NgcConstants.NGC_SUMMARY);
         extension.setDescription(description);

         ClientInfo clientInfo = new ClientInfoImpl();
         clientInfo.setCompany(NgcConstants.NGC_COMPANY);
         clientInfo.setDescription(description);
         clientInfo.setType(NgcConstants.NGC_CLIENT_TYPE);
         clientInfo.setUrl(pluginUrl);
         clientInfo.setVersion(NgcConstants.NGC_VERSION);
         ClientInfo[] clientList = { clientInfo };
         extension.setClient(clientList);

         ServerInfo serverInfo = new ServerInfoImpl();
         String[] adminEmailList = { NgcConstants.NGC_ADMIN_EMAIL };
         serverInfo.setAdminEmail(adminEmailList);
         serverInfo.setCompany(NgcConstants.NGC_COMPANY);
         serverInfo.setDescription(description);

         String thumbPrint = getCertThumbPrint();
         serverInfo.setServerThumbprint(thumbPrint);
         serverInfo.setType(NgcConstants.NGC_SERVER_TYPE);
         serverInfo.setUrl(pluginUrl);

         ServerInfo serverInfoMgmt = null;
         String mgmtMoref =
               Configuration.getString(Constants.BDE_SERVER_VM_MOBID);
         if (mgmtMoref.length() > 0) {
            serverInfoMgmt = new ServerInfoImpl();
            serverInfoMgmt.setAdminEmail(adminEmailList);
            serverInfoMgmt.setCompany(NgcConstants.NGC_COMPANY);
            serverInfoMgmt.setDescription(description);
            serverInfoMgmt.setType(mgmtMoref);
            serverInfoMgmt.setUrl(pluginUrl);
         }
         if (serverInfoMgmt == null) {
            ServerInfo[] serverList = { serverInfo };
            extension.setServer(serverList);
         } else {
            ServerInfo[] serverList = { serverInfo, serverInfoMgmt };
            extension.setServer(serverList);
         }

         GregorianCalendar calendar =
               (GregorianCalendar) Calendar.getInstance();
         extension.setLastHeartbeatTime(calendar);

         return extension;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private String getCertThumbPrint(){
		try {
			KeyStore store = JksKeyStoreUtil.loadKeyStore(Configuration.getString("ngc.extension.keystore"), Configuration.getString("cms.keystore_pswd"));
			AuAssert.check(store != null);
			Certificate cert = store.getCertificate(Configuration.getString("ngc.extension.keystore_alias"));
			String thumbPrint = CmsKeyStore.parseThumbPrint(cert);
	        LOGGER.info("Serengeti server thrumb print: "+thumbPrint);
	        return thumbPrint;
		} catch (Exception e) {
			LOGGER.error("fail to retrieve server thrumb print");
		}
		return null;
   }

   private void packageNgcTarball() throws ZipException, IOException {
      Properties properties = new Properties();

      String mgmtRefId = Configuration.getString(Constants.BDE_SERVER_VM_MOBID);
      String mgmtExtensionKey = "com.vmware.aurora.vcext.instance-";
      String serverGuid = VcContext.getServerGuid();
      String[] mgmtRefIdFields = mgmtRefId.split("-");

      if (mgmtRefIdFields.length > 1) {
         String vmId = mgmtRefIdFields[mgmtRefIdFields.length - 1];
         vmId = Integer.toHexString(Integer.parseInt(vmId));
         mgmtExtensionKey = mgmtExtensionKey + vmId;
      }
      properties.setProperty("mgmtExtensionKey", mgmtExtensionKey);
      properties.setProperty("mgmtRefId", mgmtRefId);
      properties.setProperty("serverGuid", serverGuid);

      NgcZipPacker packer = new NgcZipPacker(properties,packageName);
      packer.repack();
   }

}
