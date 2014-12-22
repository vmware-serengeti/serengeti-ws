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
package com.vmware.bdd.manager.collection;

import java.io.File;

import com.vmware.bdd.service.collection.ICollectionInitializerService;

public class FakeCollectionDriverManager extends CollectionDriverManager {

   public FakeCollectionDriverManager(String driverClass,
         ICollectionInitializerService collectionInitializerService,
         CollectionDriver collectionDirver, File configurationFile) {
      super(driverClass, collectionInitializerService);
      setDriver(collectionDirver);
      setFile(configurationFile);
   }

   private void setDriver(CollectionDriver driver) {
      super.driver = driver;
   }

   private void setFile(File configurationFile) {
      super.file = configurationFile;
   }

   @Override
   public CollectionDriver getDriver() {
      return super.getDriver();
   }

}
