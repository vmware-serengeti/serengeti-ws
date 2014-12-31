/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.exception.collection;

import com.vmware.bdd.exception.BddException;

public class CollectionDriverManagerException extends BddException {

   /**
    *
    */
   private static final long serialVersionUID = -6517368545276237210L;

   public CollectionDriverManagerException() {
   }

   public CollectionDriverManagerException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "DATA_COLLECTION", errorId, detail);
   }

   public static CollectionDriverManagerException FAILED_TO_INITIALIZE_COLLECTION_DRIVER_MANAGER() {
      return new CollectionDriverManagerException(null,
            "FAILED_TO_INITIALIZE_COLLECTION_DRIVER_MANAGER");
   }
}
