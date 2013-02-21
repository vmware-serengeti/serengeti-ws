/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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


/**
 * This class defines a inclusive interval.
 * 
 */
public class IpBlock {
   private String beginIp;
   private String endIp; // included in the interval
   
   public String getBeginIp() {
      return beginIp;
   }

   public void setBeginIp(String beginIp) {
      this.beginIp = beginIp;
   }

   public String getEndIp() {
      return endIp;
   }

   public void setEndIp(String endIp) {
      this.endIp = endIp;
   }

   public String toString() {
      if (beginIp == null && endIp == null) {
         return null;
      }
      
      StringBuilder sb = new StringBuilder();
      if (beginIp != null) {
         sb.append(beginIp);
      }
      if (endIp != null && !beginIp.equals(endIp)) {
         if (sb.length() > 0) {
            sb.append("-");
         }
         sb.append(endIp);
      }
      return sb.toString();
   }
}
