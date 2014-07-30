/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.utils.CommonUtil;

public class AmbariFactory implements SoftwareManagerFactory {

   private static final Logger logger = Logger.getLogger(AmbariFactory.class);

   @Override
   public SoftwareManager getSoftwareManager(String URL, String username,
         char[] password, String certificate) {
      URL url = null;
      try {
         url = new URL(URL);
      } catch (MalformedURLException e) {
         logger.warn("AmbariFactory:: Url parse error: " + e.getMessage());
      }
      if (url == null || CommonUtil.isBlank(url.getHost())) {
         String msg = "The format of URL  " + URL + "is invalid.";
         logger.warn(msg);
         throw new BddException(null, "SOFT_MANAGER", "INVALID_URL", msg);
      }
      return new AmbariImpl(url, username, new String(password), certificate);
   }

}
