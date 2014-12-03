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

import java.security.cert.X509Certificate;


/**
 * Created By xiaoliangl on 12/1/14.
 */
public final class PspConfigurationForConsole implements TrustCertCallBack {
   private boolean forceTrustCert = false;

   PspConfigurationForConsole(boolean forceTrustCert1) {
      forceTrustCert = forceTrustCert1;
   }


   @Override
   public boolean doTrustOnFirstUse(X509Certificate leafCert) {
      /**
       * Present user with certificate and add it as an exception
       * only if the user manually inspects the certificate and
       * decides to trust it.
       */
      TlsHelper.presentUserWithCert(TlsHelper.getCertificateInfo(leafCert), System.out);


      System.out.println("Do you wish to trust this certificate?");
      String readMsg;
      if (!forceTrustCert) {
         readMsg = TlsHelper.getUserInput();
      } else {
         readMsg = "yes";
      }

      if ("yes".equals(readMsg) || "y".equals(readMsg)) {
         return true;
      }

      return true;
   }
}
