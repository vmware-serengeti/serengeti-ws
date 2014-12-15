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

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * @TODO add persistence
 * Created By xiaoliangl on 11/28/14.
 */
@Component
public class UserMgmtServerEao {
   Map<String, UserMgmtServer> usrMgmtServerMap = new HashMap<>();

   public void persist(UserMgmtServer usrMgmtServer) {
      if(usrMgmtServerMap.containsKey(usrMgmtServer.getName())) {
         ValidationError validationError = new ValidationError("NAME.DUPLICATION", "Same name already exists");
         ValidationErrors errors = new ValidationErrors();
         errors.addError("Name", validationError);
         throw new ValidationException(errors.getErrors());
      }

      usrMgmtServerMap.put(usrMgmtServer.getName(), usrMgmtServer);
   }

   public UserMgmtServer findByName(String name) {
      return usrMgmtServerMap.get(name);
   }

   public void delete(String name) {
      if(usrMgmtServerMap.containsKey(name)) {
         usrMgmtServerMap.remove(name);
      } else {
         ValidationError validationError = new ValidationError("NAME.NOT_FOUND", "given name not found.");
         ValidationErrors errors = new ValidationErrors();
         errors.addError("Name", validationError);
         throw new ValidationException(errors.getErrors());
      }

   }
}
