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
package com.vmware.bdd.plugin.clouderamgr.poller.host.parser;

/**
 * Author: Xiaoding Bian
 * Date: 6/26/14
 * Time: 6:34 PM
 */
public class ParseResult {

   private int percent;

   private String message;

   public int getPercent() {
      return percent;
   }

   public void setPercent(int percent) {
      this.percent = percent;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || o.getClass() != getClass()) {
         return false;
      }
      ParseResult other = (ParseResult) o;
      if (this.getPercent() != other.getPercent()) {
         return false;
      }
      if (this.getMessage() == null && other.getMessage() != null) {
         return false;
      }
      if (this.getMessage() != null && !this.getMessage().equals(other.getMessage())) {
         return false;
      }
      return true;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getPercent();
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      return result;
   }
}
