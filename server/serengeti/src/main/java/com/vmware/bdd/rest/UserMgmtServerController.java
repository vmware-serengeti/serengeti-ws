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
 *****************************************************************************/
package com.vmware.bdd.rest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.usermgmt.UserMgmtServerService;

/**
 * Created By xiaoliangl on 11/24/14.
 */
@Controller
@RequestMapping(value = "vmconfig/usermgmtservers")
public class UserMgmtServerController {

   @Autowired
   private UserMgmtServerService userMgmtServerService;


   @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
   @ResponseBody
   public void add(@RequestBody @Valid UserMgmtServer serverInfo,
                   @RequestParam(value = "testonly", required = false) boolean testOnly,
                   @RequestParam(value = "forceTrustCert", required = false) boolean certConfirmed) {
      userMgmtServerService.add(serverInfo, testOnly, certConfirmed);
   }

   @RequestMapping(value = "/{name}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
   @ResponseBody
   public void modify(
         @PathVariable("name") @Valid @NotNull String name,
         @RequestBody @Valid UserMgmtServer serverInfo,
         @RequestParam(value = "testonly", required = false) boolean testOnly,
         @RequestParam(value = "forceTrustCert", required = false) boolean certConfirmed) {
      serverInfo.setName(name);

      userMgmtServerService.modify(serverInfo, testOnly, certConfirmed);
   }

   @RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public UserMgmtServer getByName(@PathVariable("name") @Valid @NotNull String name) {
//      UserMgmtServer userMgmtServer = new UserMgmtServer("ldap1", UserMgmtServer.Type.valueOf("LDAP"), "cn=groups,dn=vmware,dn=com",
//            "cn=users,dn=vmware,dn=com", "ldaps://10.112.113.137:389", "ldaps://10.112.113.182:389", "xiaoliangl", "password");

      return userMgmtServerService.getByName(name, true);
   }

   @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   public void delete(@PathVariable("name") @Valid @NotNull String name) {
      userMgmtServerService.deleteByName(name);
   }


   /*@RequestMapping(method = RequestMethod.GET, produces = "application/json")
   public List<UserMgmtServer> list() {
      return null;
   }*/
}
