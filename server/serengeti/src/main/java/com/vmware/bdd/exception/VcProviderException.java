/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.exception;

import java.util.List;

public class VcProviderException extends BddException {

   private static final long serialVersionUID = 1L;

   public VcProviderException() {
   }

   public VcProviderException(Throwable cause, String errorId, Object... detail) {
      super(cause, "VC_PROVIDER", errorId, detail);
   }

   public static VcProviderException VC_RESOURCE_POOL_ALREADY_ADDED(String resourcePoolName) {
      return new VcProviderException(null, "VC_RESOURCE_POOL_ALREADY_ADDED", resourcePoolName);
   }

   public static VcProviderException NO_RESOURCE_POOL_FOUND(String[] names) {
      return new VcProviderException(null, "NO_RESOURCE_POOL_FOUND", (Object[])names);
   }

   public static VcProviderException RESOURCE_POOL_NOT_FOUND(String rpName) {
      return new VcProviderException(null, "RESOURCE_POOL_NOT_FOUND", rpName);
   }

   public static VcProviderException DATASTORE_NOT_FOUND(String dsName) {
      return new VcProviderException(null, "DATASTORE_NOT_FOUND", dsName);
   }

   public static VcProviderException DATASTORE_IS_REFERENCED_BY_CLUSTER(List<String> clusterNames) {
      return new VcProviderException(null, "DATASTORE_IS_REFERENCED_BY_CLUSTER", clusterNames);
   }

   public static VcProviderException RESOURCE_POOL_IS_REFERENCED_BY_CLUSTER(List<String> clusterNames) {
      return new VcProviderException(null, "RESOURCE_POOL_IS_REFERENCED_BY_CLUSTER", clusterNames);
   }
}
