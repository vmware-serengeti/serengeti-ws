/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.impl;

import org.apache.log4j.Logger;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.IScaleService;
import com.vmware.bdd.service.sp.ScaleVMSP;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ScaleService implements IScaleService {
   private static final Logger logger = Logger.getLogger(ScaleService.class);

   private ClusterEntityManager clusterEntityMgr;


   /**
    * @return the clusterEntityMgr
    */
   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }


   /**
    * @param clusterEntityMgr
    *           the clusterEntityMgr to set
    */
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.IScaleService#scaleNodeResource(java.lang.String, int, long)
    */
   @Override
   public boolean scaleNodeResource(String nodeName, int cpuNumber, long memory) {
      logger.info("scale node: " + nodeName + ", cpu number: " + cpuNumber
            + ", memory: " + memory);
      NodeEntity node = clusterEntityMgr.findNodeByName(nodeName);
      ScaleVMSP scaleVMSP = new ScaleVMSP(node.getMoId(), cpuNumber, memory);
      boolean vmResult =  VcVmUtil.runSPOnSingleVM(node, scaleVMSP);
      if(vmResult){
         if(cpuNumber > 0){
            node.setCpuNum(cpuNumber);
         }
         if(memory > 0){
            node.setMemorySize(memory);
         }
         clusterEntityMgr.update(node);
      }
      return vmResult;
   }

}
