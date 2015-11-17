/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.utils;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.exception.WarningMessageException;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import org.springframework.dao.DataAccessException;

/**
 * Created by xiaoliangl on 11/16/15.
 */
public class ServerErrorMessages {
   public static BddErrorMessage fromException(BddException bddEx) {
      BddErrorMessage errorMessage = new BddErrorMessage(bddEx.getFullErrorId(), extractErrorMessage(bddEx));

      if(bddEx instanceof WarningMessageException) {
         errorMessage.setWarning(true);
      }

      if(bddEx instanceof ValidationException) {
         errorMessage.setErrors(((ValidationException) bddEx).getErrors());
      }

      if(bddEx instanceof UntrustedCertificateException) {
         errorMessage.setCertInfo(((UntrustedCertificateException) bddEx).getCertInfo());
      }

      return errorMessage;
   }

   private static String extractErrorMessage(BddException ex) {
      String msg = ex.getMessage();
      if (ex.getCause() instanceof DataAccessException) {
         msg = "Data access layer exception. See the detailed error in the log";
      }
      return msg;
   }
}
