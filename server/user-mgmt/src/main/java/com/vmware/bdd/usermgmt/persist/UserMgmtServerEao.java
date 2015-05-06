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
package com.vmware.bdd.usermgmt.persist;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.dal.IBaseDAO;
import com.vmware.bdd.exception.EncryptionException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.EncryptionGuard;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
@Transactional(propagation = Propagation.REQUIRED)
public class UserMgmtServerEao {
   @Autowired
   @Qualifier(value = "userMgmtServerDao")
   private IBaseDAO<UserMgmtServerEntity> userMgmtServerDao;

   public void persist(UserMgmtServer usrMgmtServer) {
      encryptPassword(usrMgmtServer);

      UserMgmtServerEntity userMgmtServerEntity = userMgmtServerDao.findById(usrMgmtServer.getName());

      if (userMgmtServerEntity != null) {
         ValidationError validationError = new ValidationError("NAME.DUPLICATION", "Same name already exists");
         ValidationErrors errors = new ValidationErrors();
         errors.addError("Name", validationError);
         throw new ValidationException(errors.getErrors());
      }

      userMgmtServerEntity = new UserMgmtServerEntity();
      userMgmtServerEntity.copyFrom(usrMgmtServer);

      userMgmtServerDao.insert(userMgmtServerEntity);
   }

   @Transactional(propagation = Propagation.SUPPORTS)
   public UserMgmtServer findByName(String name, boolean safely) {
      UserMgmtServerEntity userMgmtServerEntity = userMgmtServerDao.findById(name);

      UserMgmtServer userMgmtServer = userMgmtServerEntity == null ? null : userMgmtServerEntity.copyTo();

      if(!safely) {
         decryptPassword(userMgmtServer);
      }

      return userMgmtServer;
   }

   public void delete(String name) {
      UserMgmtServerEntity userMgmtServerEntity = userMgmtServerDao.findById(name);

      if (userMgmtServerEntity != null) {
         userMgmtServerDao.delete(userMgmtServerEntity);
      } else {
         ValidationError validationError = new ValidationError("NAME.NOT_FOUND", "given name not found.");
         ValidationErrors errors = new ValidationErrors();
         errors.addError("Name", validationError);
         throw new ValidationException(errors.getErrors());
      }

   }

   public void checkServerChanged(UserMgmtServer userMgtServer) {
      UserMgmtServer existingUserMgmtServer = findByName(userMgtServer.getName(), false);

      ValidationErrors errors = new ValidationErrors();
      if (existingUserMgmtServer == null) {
         ValidationError validationError = new ValidationError("NAME.NOT_FOUND", "given server is not found.");
         errors.addError("NAME", validationError);
      }

      if (existingUserMgmtServer !=null && existingUserMgmtServer.equals(userMgtServer)) {
         ValidationError validationError = new ValidationError("USERMGMTSERVER.NO_CHANGE", "The server info is not changed.");
         errors.addError("USERMGMTSERVER", validationError);
      }

      if (!errors.getErrors().isEmpty()) {
         throw new ValidationException(errors.getErrors());
      }
   }

   public void modify(UserMgmtServer usrMgmtServer) {
      encryptPassword(usrMgmtServer);

      UserMgmtServerEntity userMgmtServerEntity = userMgmtServerDao.findById(usrMgmtServer.getName());
      userMgmtServerEntity.copyFrom(usrMgmtServer);
      userMgmtServerDao.update(userMgmtServerEntity);
   }

   private void encryptPassword(UserMgmtServer userMgtServer) {
      String encryptedPassword = null;
      try {
         encryptedPassword = EncryptionGuard.encode(userMgtServer.getPassword());
      } catch (EncryptionException | GeneralSecurityException | UnsupportedEncodingException e) {
         throw new UserMgmtPersistException("USER_MGMT_SERVER.PASSWORD_ENCRYPT_FAIL", e);
      }
      userMgtServer.setPassword(encryptedPassword);
   }

   private void decryptPassword(UserMgmtServer userMgmtServer) {

      try {
         if (userMgmtServer != null) {
            userMgmtServer.setPassword(EncryptionGuard.decode(userMgmtServer.getPassword()));
         }
      } catch (EncryptionException | GeneralSecurityException | UnsupportedEncodingException e) {
         throw new UserMgmtPersistException("USER_MGMT_SERVER.PASSWORD_DECRYPT_FAIL", e);
      }
   }
}
