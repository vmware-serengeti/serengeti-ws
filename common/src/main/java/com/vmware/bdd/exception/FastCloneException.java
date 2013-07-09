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


/**
 * fast clone exception defintions
 * 
 * @author tli
 * 
 */
public class FastCloneException extends BddException {
   private static final long serialVersionUID = 1L;

   public FastCloneException() {
   }

   public FastCloneException(Throwable cause, String errorId, Object... detail) {
      super(cause, "FAST_CLONE_SERVICE", errorId, detail);
   }

   public FastCloneException VM_CLONE_ERROR(String clusterName) {
      return new FastCloneException(null, "VM_FAST_CLONE_ERROR", clusterName);
   }
}