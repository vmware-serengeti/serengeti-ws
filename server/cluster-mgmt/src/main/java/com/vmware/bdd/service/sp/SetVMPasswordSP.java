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
package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.bdd.service.ISetPasswordService;
/**
 * Store Procedure of setting password for a vm
 */
public class SetVMPasswordSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(SetVMPasswordSP.class);
   private String clusterName;
   private String nodeIP;

   private String password;
   private ISetPasswordService setPasswordService;

   public SetVMPasswordSP(String clusterName, String nodeIP, String password) {
      this.clusterName = clusterName;
      this.nodeIP = nodeIP;
      this.password = password;
   }

   @Override
   public Void call() throws Exception {
      setPasswordService.setPasswordForNode(clusterName, nodeIP, password);
      return null;
   }

   public String getNodeIP() {
      return nodeIP;
   }

   public void setNodeIP(String nodeIP) {
      this.nodeIP = nodeIP;
   }
}
