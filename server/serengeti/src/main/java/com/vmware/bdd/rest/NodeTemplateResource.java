/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.bdd.apitypes.VirtualMachineRead;
import com.vmware.bdd.service.resmgmt.INodeTemplateService;

@Controller
public class NodeTemplateResource {

   @Autowired
   private INodeTemplateService nodeTemplateService;

   @RequestMapping(value = "/templates", method = RequestMethod.GET, produces = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @ResponseBody
   public List<VirtualMachineRead> getNodeTemplates() {
      return nodeTemplateService.getAllNodeTemplates();
   }
}
