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
package com.vmware.bdd.cli.http;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.springframework.stereotype.Component;

/**
 * Created by xiaoliangl on 9/16/15.
 */
@Component
public class HostnameVerifiers {
   public static final String ALLOW_ALL_HOSTNAME_VERIFIER = "allow_all";

   public static final String BROWSER_COMPATIBLE_HOSTNAME_VERIFIER = "browser_compatible";

   public static final String STRICT_HOSTNAME_VERIFIER = "strict";

   public X509HostnameVerifier getHostnameVerifier(String verifier) {
      switch (verifier) {
         case "browser_compatible":
            return SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
         case "strict":
            return SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
         case "allow_all":
         default:
            return SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
      }

   }
}
