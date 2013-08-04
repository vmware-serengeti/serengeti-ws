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
package com.vmware.bdd.exception;

public class ScaleServiceException extends BddException {

   private static final long serialVersionUID = 1l;

   public ScaleServiceException() {
   }

   public ScaleServiceException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SCALE_SERVICE", errorId, detail);
   }

   public static ScaleServiceException CURRENT_DATASTORE_UNACCESSIBLE(
         String dsName) {
      return new ScaleServiceException(null, "CURRENT_DATASTORE_UNACCESSIBLE",
            dsName);
   }

   public static ScaleServiceException NOT_NEEDED(String clusterName) {
      return new ScaleServiceException(null, "NOT_NEEDED", clusterName);
   }

   public static ScaleServiceException JOB_LAUNCH_FAILURE(String clusterName,
         Throwable t, String errorMsg) {
      return new ScaleServiceException(t, "JOB_LAUNCH_FAILURE", clusterName,
            errorMsg);
   }

   public static ScaleServiceException COMMON_SCALE_ERROR(String vmName) {
      return new ScaleServiceException(null, "COMMON", vmName);
   }
}
