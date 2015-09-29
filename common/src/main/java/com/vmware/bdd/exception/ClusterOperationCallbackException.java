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

public class ClusterOperationCallbackException extends BddException {
   private static final long serialVersionUID = 1l;

   ClusterOperationCallbackException(){}

   public ClusterOperationCallbackException(Throwable cause, String errorId, Object... detail) {
      super(cause, "CLUSTER_OPERATION_CALLBACK", errorId, detail);
   }

   public static ClusterOperationCallbackException FAIL_TO_EXECUTE_CLUSTER_OPERATION_CALLBACK(String command, String msg) {
      return new ClusterOperationCallbackException(null, "FAIL_TO_EXECUTE_CLUSTER_OPERATION_CALLBACK", command, msg);
   }
}
