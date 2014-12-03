/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.security.tls;

import com.vmware.bdd.exception.BddException;

/**
 * Created By xiaoliangl on 12/1/14.
 */
public class TlsConnectionException extends BddException{
   private boolean connectionFail = false;

   public TlsConnectionException(Throwable throwable, String error, boolean connectFail) {
      super(throwable, "BDD.SECURITY", "TLS_CONNECTION_ERR", error);

      connectionFail = connectFail;
   }

   public boolean isConnectionFail() {
      return connectionFail;
   }

}
