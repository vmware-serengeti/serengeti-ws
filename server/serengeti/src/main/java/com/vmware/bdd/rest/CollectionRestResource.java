/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.bdd.exception.collection.CollectionDriverManagerException;
import com.vmware.bdd.manager.collection.CollectionDriverManager;

@Controller
public class CollectionRestResource {

   private static final Logger logger = Logger.getLogger(CollectionRestResource.class);
   @Autowired
   private CollectionDriverManager collectionDriverManager;

   @RequestMapping(value = "/datacollector", method = RequestMethod.GET)
   @ResponseStatus(HttpStatus.OK)
   @ResponseBody
   public boolean getCollectionSwitchStatus() {
      if (collectionDriverManager == null) {
         return false;
      } else {
         return collectionDriverManager.getCollectionSwitchStatus();
      }
   }

   @RequestMapping(value = "/datacollector", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void changeCollectionSwitchStatus(
         @RequestParam(value = "enabled", required = true) Boolean enabled) {
      if (collectionDriverManager == null) {
         throw CollectionDriverManagerException
               .FAILED_TO_INITIALIZE_COLLECTION_DRIVER_MANAGER();
      } else {
         collectionDriverManager.changeCollectionSwitchStatus(enabled);
      }
   }
}
