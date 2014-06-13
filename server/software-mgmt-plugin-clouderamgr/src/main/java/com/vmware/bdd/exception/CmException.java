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
package com.vmware.bdd.exception;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 1:13 PM
 */
public class CmException extends BddException {

   public CmException() {
   }

   public CmException(Object... detail) {
      this(null, detail);
   }

   public CmException(Throwable cause, Object... detail) {
      this(cause, "UNKNOWN", detail);
   }


   public CmException(Throwable cause, String errorId, Object... detail) {
      super(cause, "CLOUDERA_MANAGER", errorId, detail);
   }

   public CmException(String msg) {
      this(null, "UNKNOWN", msg);
   }


   public static CmException INVALID_VERSION(String version) {
      return new CmException(null, "INVALID_VERSION", version);
   }

   public static CmException UNSURE_CLUSTER_EXIST(String clusterName) {
      return new CmException(null, "UNSURE_CLUSTER_EXIST", clusterName);
   }

   public static CmException CREATE_CLUSTER_FAILED(Exception e, String clusterName) {
      return new CmException(e, "CREATE_CLUSTER_FAILED", clusterName);
   }
}
