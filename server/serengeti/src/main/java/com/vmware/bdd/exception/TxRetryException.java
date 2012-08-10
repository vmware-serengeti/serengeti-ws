/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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


public class TxRetryException extends DalException {
   private static final long serialVersionUID = 1L;

   public TxRetryException() {
   }

   public TxRetryException(Throwable cause, String errorId, Object... detail) {
      super(cause, errorId, detail);
   }

   public static TxRetryException wrap(Throwable ex, boolean rndRollback) {
      return new TxRetryException(ex, "TX_RETRY_LIMIT_EXCEEDS", rndRollback ? "on"
            : "off");
   }
}
