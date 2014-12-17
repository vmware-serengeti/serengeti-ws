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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.dal.IBaseDAO;
import com.vmware.bdd.exception.ValidationException;
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
   public UserMgmtServer findByName(String name) {
      UserMgmtServerEntity userMgmtServerEntity = userMgmtServerDao.findById(name);

      return userMgmtServerEntity == null ? null : userMgmtServerEntity.copyTo();
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
}
