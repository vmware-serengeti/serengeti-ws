/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.cli.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.DatastoreAdd;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtMode;
import com.vmware.bdd.utils.CommonUtil;

@Component
public class UserMgmtServerRestClient {
   private final static String USERMGMTSERVER_URL = "vmconfig/usermgmtservers";

   @Autowired
   private RestClient restClient;

   public void addUserMgmtServer(UserMgmtServer userMgmtServer, boolean forceTrustCert) {
      StringBuilder queryBuilder = new StringBuilder(USERMGMTSERVER_URL)
            .append("?").append("forceTrustCert").append('=').append(forceTrustCert);

      restClient.createObject(userMgmtServer, queryBuilder.toString(), HttpMethod.POST);
   }

   public void modifyUserMgmtServer(UserMgmtServer userMgmtServer, boolean forceTrustCert) {
      StringBuilder queryBuilder = new StringBuilder(USERMGMTSERVER_URL)
            .append("/").append(userMgmtServer.getName())
            .append("?").append("forceTrustCert").append('=').append(forceTrustCert);
      restClient.update(userMgmtServer, queryBuilder.toString(), HttpMethod.PUT);
   }

   public UserMgmtServer get(String name) {
      return restClient.getObject(name, UserMgmtServer.class, USERMGMTSERVER_URL, HttpMethod.GET, false);
   }

   public void removeUserMgmtServer(String name) {
      restClient.deleteObject(name, USERMGMTSERVER_URL, HttpMethod.DELETE);
   }

}
