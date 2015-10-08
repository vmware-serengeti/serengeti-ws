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
package com.vmware.bdd.cli.config;

import com.vmware.bdd.cli.commands.CliException;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.http.HostnameVerifiers;
import com.vmware.bdd.security.tls.PspConfiguration;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Created by xiaoliangl on 9/16/15.
 */
@Component
public class CliProperties {
   private static Logger LOGGER = Logger.getLogger(CliProperties.class);

   private static final String KEY_STORE_PASSWORD_KEY = "keystore_pswd";
   private static final int KEY_STORE_PASSWORD_LENGTH = 8;
   private static final String SUPPORTED_PROTOCOLS = "supported_protocols";
   private static final String SUPPORTED_CIPHERSUITES = "supported_ciphersuites";
   private static final String SUPPORTED_CIPHERSUITES_GROUP = "supported_ciphersuites_group";
   private static final String HOSTNAME_VERIFIER = "hostname_verifier";
   public static final String STRONG_ENCRYPTION = "STRONG_ENCRYPTION";
   public static final String DEFAULT_ENCRYPTION = "DEFAULT_ENCRYPTION";

   PropertiesConfiguration properties = new PropertiesConfiguration();

   @Autowired
   private CliSecureFilesInitiator filesInitiator;

   @PostConstruct
   protected void loadFromFile() throws CliException {
      File file = new File(Constants.PROPERTY_FILE);
      properties.setFile(file);
      if (file.isFile()) {
         try {
            properties.load();
         } catch (ConfigurationException e) {
            LOGGER.error("failed to load cli.properties", e);
            throw new CliException("failed to load cli.properties");
         }
      }
   }

   public char[] readKeyStorePwd() throws IOException, ConfigurationException {
      String password = properties.getString(KEY_STORE_PASSWORD_KEY);
      if (password == null) {
         // generate a random keystore password
         password = CommonUtil.randomString(KEY_STORE_PASSWORD_LENGTH);
      }
      return password.toCharArray();
   }

   public void saveKeyStorePwd(char[] password) throws IOException, ConfigurationException {
      properties.setProperty(KEY_STORE_PASSWORD_KEY, new String(password));
      properties.save();
   }

   public String[] getSupportedProtocols() {
      String[] values = properties.getStringArray(SUPPORTED_PROTOCOLS);
      return ArrayUtils.isEmpty(values) ? PspConfiguration.SSL_PROTOCOLS : values;
   }

   public String[] getSupportedCipherSuites() {
      String[] values = properties.getStringArray(SUPPORTED_CIPHERSUITES);
      return ArrayUtils.isEmpty(values) ? getSupportedCipherSuitesByGroup() : values;
   }

   public String[] getSupportedCipherSuitesByGroup() {
      String groupName =  properties.getString(SUPPORTED_CIPHERSUITES_GROUP);

      if (StringUtils.isBlank(groupName)) {
         LOGGER.warn(String.format("cipher suite group not set, take default one: %1s", DEFAULT_ENCRYPTION));
         groupName = DEFAULT_ENCRYPTION;
      }
      switch (groupName) {
         case STRONG_ENCRYPTION:
            return PspConfiguration.CIPHER_SUITES;
         case DEFAULT_ENCRYPTION:
            return PspConfiguration.WEAK_CIPHER_SUITES;
         default: {
            LOGGER.warn(String.format("cipher suite group can only be: %1s or %2s, but meet %3s.", STRONG_ENCRYPTION, DEFAULT_ENCRYPTION, groupName));
            return PspConfiguration.WEAK_CIPHER_SUITES;
         }
      }
   }

   public String getHostnameVerifier() {
      String value = properties.getString(HOSTNAME_VERIFIER);
      return StringUtils.isBlank(value) ? HostnameVerifiers.ALLOW_ALL_HOSTNAME_VERIFIER : value;
   }
}
