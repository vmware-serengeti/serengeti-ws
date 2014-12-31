/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
 *****************************************************************************/
package com.vmware.bdd.usermgmt;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.EncryptionException;
import com.vmware.bdd.security.EncryptionGuard;
import com.vmware.bdd.usermgmt.persist.UserMgmtPersistException;
import com.vmware.bdd.usermgmt.persist.UserMgmtServerEao;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
public class UserMgmtServerService {
   @Autowired
   private UserMgmtServerEao serverEao;

   @Autowired
   private UserMgmtServerValidService serverValidService;

   @Autowired
   private UserMgmtServerModificationHandler modificationHandler;

   public void add(UserMgmtServer userMgtServer, boolean testOnly, boolean forceTrustCert) {
      serverValidService.validateServerInfo(userMgtServer, forceTrustCert);

      if (!testOnly) {
         serverEao.persist(userMgtServer);
      }
   }

   public UserMgmtServer getByName(String name, boolean safely) {
      UserMgmtServer userMgmtServer = serverEao.findByName(name, safely);

      return userMgmtServer;
   }

   public void deleteByName(String name) {
      serverEao.delete(name);
   }

   public void modify(UserMgmtServer userMgtServer, boolean testOnly, boolean forceTrustCert) {
      serverEao.checkServerChanged(userMgtServer);

      serverValidService.validateServerInfo(userMgtServer, forceTrustCert);

      if (!testOnly) {
         modificationHandler.onModification(userMgtServer);

         //password is encrypted here.
         serverEao.modify(userMgtServer);
      }
   }
}
