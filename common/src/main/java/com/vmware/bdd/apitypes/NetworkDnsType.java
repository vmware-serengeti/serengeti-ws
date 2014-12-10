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
package com.vmware.bdd.apitypes;

public enum NetworkDnsType {

   OTHERS,
   NORMAL,
   DYNAMIC;

   public static boolean isOthers(String dnsType) {
      boolean isOthers = false;
      if (NetworkDnsType.valueOf("OTHERS").toString().equalsIgnoreCase(dnsType)) {
         isOthers = true;
      }
      return isOthers;
   }

   public static boolean isNormal(String dnsType) {
      boolean isNormal = false;
      if (NetworkDnsType.valueOf("NORMAL").toString().equalsIgnoreCase(dnsType)) {
         isNormal = true;
      }
      return isNormal;
   }

   public static boolean isDynamic(String dnsType) {
      boolean isDynamic = false;
      if (NetworkDnsType.valueOf("DYNAMIC").toString().equalsIgnoreCase(dnsType)) {
         isDynamic = true;
      }
      return isDynamic;
   }

}
