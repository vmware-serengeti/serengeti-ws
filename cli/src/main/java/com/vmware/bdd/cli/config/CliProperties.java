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

import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.http.HostnameVerifiers;
import com.vmware.bdd.security.tls.PspConfiguration;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Created by xiaoliangl on 9/16/15.
 */
@Component
public class CliProperties {
   private static final String KEY_STORE_PASSWORD_KEY = "keystore_pswd";
   private static final int KEY_STORE_PASSWORD_LENGTH = 8;
   private static final String SUPPORTED_PROTOCOLS = "supported_protocols";
   private static final String SUPPORTED_CIPHERSUITES = "supported_ciphersuites";
   private static final String HOSTNAME_VERIFIER = "hostname_verifier";

   PropertiesConfiguration properties = new PropertiesConfiguration();

   @PostConstruct
   protected void loadFromFile() throws ConfigurationException {
      File file = new File(Constants.PROPERTY_FILE);
      properties.setFile(file);
      if (file.isFile()) {
         properties.load();
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

      // set file permission to 600 to protect keystore password
      CommonUtil.setOwnerOnlyReadWrite(Constants.PROPERTY_FILE);
      CommonUtil.setOwnerOnlyReadWrite(Constants.CLI_HISTORY_FILE);
   }

   public String[] getSupportedProtocols() {
      String[] values = properties.getStringArray(SUPPORTED_PROTOCOLS);
      return ArrayUtils.isEmpty(values) ? PspConfiguration.SSL_PROTOCOLS : values;
   }

   public String[] getSupportedCipherSuites() {
      String[] values = properties.getStringArray(SUPPORTED_CIPHERSUITES);
      return ArrayUtils.isEmpty(values) ? PspConfiguration.WEAK_CIPHER_SUITES : values;
   }

   public String getHostnameVerifier() {
      String value = properties.getString(HOSTNAME_VERIFIER);
      return StringUtils.isBlank(value) ? HostnameVerifiers.ALLOW_ALL_HOSTNAME_VERIFIER : value;
   }
}
