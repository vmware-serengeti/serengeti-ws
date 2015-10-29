/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.collection.impl;

import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.resmgmt.INetworkService;

public class FakeTimelyCollectionService extends TimelyCollectionService {

    public ClusterManager getClusterMgr() {
        return super.clusterMgr;
    }

    public void setClusterMgr(ClusterManager clusterMgr) {
        super.clusterMgr = clusterMgr;
    }

    public IClusterEntityManager getClusterEntityMgr() {
        return super.clusterEntityMgr;
    }

    public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
        super.clusterEntityMgr = clusterEntityMgr;
    }

    public SoftwareManagerCollector getSoftwareManagerCollector() {
        return super.softwareManagerCollector;
    }

    public void setSoftwareManagerCollector(SoftwareManagerCollector softwareManagerCollector) {
        super.softwareManagerCollector = softwareManagerCollector;
    }

    public INetworkService getNetworkService() {
        return super.networkService;
    }

    public void setNetworkService(INetworkService networkService) {
        super.networkService = networkService;
    }
}
