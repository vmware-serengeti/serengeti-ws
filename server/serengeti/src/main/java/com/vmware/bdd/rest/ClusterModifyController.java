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
package com.vmware.bdd.rest;

import com.vmware.bdd.aop.annotation.RestCallPointcut;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.service.impl.ClusteringService;
import com.vmware.bdd.utils.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


/**
 * Created By jiahuili on 07/15/15.
 */
@Controller
public class ClusterModifyController {

   @Autowired
   private ClusterManager clusterMgr;

   @RequestMapping(value = "/cluster/{clusterName}/update", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
   public void modifyCluster(@PathVariable("clusterName") String clusterName, @RequestBody ClusterCreate clusterModify,
         @RequestParam(value = "warningforce", required = false) boolean warningforce) throws Exception{
      verifyInitialized();
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateResourceName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      clusterMgr.modifyCluster(clusterModify, warningforce);
   }

   private void verifyInitialized() {
      if (!ClusteringService.isInitialized()) {
         throw BddException.INIT_VC_FAIL();
      }
   }

}
