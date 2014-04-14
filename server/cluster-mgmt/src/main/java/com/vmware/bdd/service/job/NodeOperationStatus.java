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
package com.vmware.bdd.service.job;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class NodeOperationStatus {

   private String nodeName;
   private boolean succeed = true;
   private String errorMessage;

   public NodeOperationStatus() {}

   public NodeOperationStatus(String nodeName) {
      this.nodeName = nodeName;
   }

   public NodeOperationStatus(String nodeName, boolean succeed, String errorMessage) {
      this(nodeName);
      this.succeed = succeed;
      this.errorMessage = errorMessage;
   }

   /**
    * @return the nodeName
    */
   public String getNodeName() {
      return nodeName;
   }

   /**
    * @param nodeName
    *           the nodeName to set
    */
   public void setNodeName(String nodeName) {
      this.nodeName = nodeName;
   }

   /**
    * @return the succeed
    */
   public boolean isSucceed() {
      return succeed;
   }

   /**
    * @param succeed
    *           the succeed to set
    */
   public void setSucceed(boolean succeed) {
      this.succeed = succeed;
   }

   /**
    * @return the errorMessage
    */
   public String getErrorMessage() {
      return errorMessage;
   }

   /**
    * @param errorMessage
    *           the errorMessage to set
    */
   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }

   @Override
   public String toString() {
      StringBuilder strBuilder = new StringBuilder();
      strBuilder.append("nodeName:");
      strBuilder.append(nodeName);
      strBuilder.append(",succeed:");
      strBuilder.append(succeed);
      if (errorMessage != null) {
         strBuilder.append(",error message:");
         strBuilder.append(errorMessage);
      }
      return strBuilder.toString();
   }

}
