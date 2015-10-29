/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.collection;

import java.util.Map;

import com.vmware.bdd.apitypes.DataObjectType;

public interface ITimelyCollectionService extends ICollectionService {

   Map<String, Map<String, Object>> collectData(Map<String, Object> data,
         DataObjectType operation);

   Map<String, Map<String, ?>> mergeData(Map<String, Map<String, Object>> operationdata,
         Map<String, Map<String, Object>> clusterSnapshotData);

}
