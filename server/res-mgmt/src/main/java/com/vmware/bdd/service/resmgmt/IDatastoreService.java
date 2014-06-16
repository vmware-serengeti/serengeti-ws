/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.resmgmt;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.apitypes.DatastoreAdd;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;


/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public interface IDatastoreService {

   Set<String> getAllSharedDatastores();

   Set<String> getSharedDatastoresByNames(List<String> nameList);

   Set<String> getAllLocalDatastores();

   Set<String> getLocalDatastoresByNames(List<String> nameList);

   Set<String> getDatastoresByName(String name);

   Set<String> getDatastoresByNames(List<String> nameList);

   // for CLI path
   Set<String> getAllDatastoreNames();

   // for CLI path
   DatastoreRead getDatastoreRead(String name);

   // for CLI path
   List<DatastoreRead> getAllDatastoreReads();

   void addDatastores(DatastoreAdd datastore);

   void addDatastores(String name, DatastoreType type, List<String> spec, boolean regex);

   void deleteDatastore(String name);
}