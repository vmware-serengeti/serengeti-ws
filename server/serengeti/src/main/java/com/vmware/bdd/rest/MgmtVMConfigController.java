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

import java.util.Map;

import com.vmware.bdd.aop.annotation.RestCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.bdd.usermgmt.MgmtVmCfgService;

/**
 * Created By xiaoliangl on 11/24/14.
 */
@Controller
@RequestMapping(value = "vmconfig/mgmtvm")
public class MgmtVMConfigController {
   @Autowired
   private MgmtVmCfgService mgmtVmCfgService;

   @RequestMapping(method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCall
   public Map<String, String> get() {
      return mgmtVmCfgService.get();
   }

   @RequestMapping(method = RequestMethod.PUT, consumes = "application/json")
   @ResponseBody
   @RestCall
   public void modify(@RequestBody Map<String, String> config) {
      mgmtVmCfgService.config(config);
   }

   /*
   @RequestMapping(value = "{name}", method = RequestMethod.PUT)
   public void modify(@PathVariable("name") String name, String value) {

   }

   @RequestMapping(value = "{name}", method = RequestMethod.GET, produces = "application/json")
   public String getByName(@PathVariable("name") String name) {
      return null;
   }*/
}
