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
package com.vmware.bdd.clone.spec;

/**
 * vm creation result
 *
 * @author line
 *
 */
public class VmCreateResult<T extends Location> {

   private T spec;

   private String errMessage;

   private String errTimestamp;

   private boolean success;

   public T getSpec() {
      return spec;
   }

   public void setSpec(T spec) {
      this.spec = spec;
   }

   public String getErrMessage() {
      return errMessage;
   }

   public void setErrMessage(String errMessage) {
      this.errMessage = errMessage;
   }

   public String getErrTimestamp() {
      return errTimestamp;
   }

   public void setErrTimeStamp(String errTimestamp) {
      this.errTimestamp = errTimestamp;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }
}
