/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.dal;

import java.util.List;

import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.NetworkEntity;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public interface INetworkDAO extends IBaseDAO<NetworkEntity> {

   NetworkEntity findNetworkByName(String name);

   List<NetworkEntity> findAllNetworks();

   List<IpBlockEntity> findAllIpBlocks(NetworkEntity entity);

   List<IpBlockEntity> findAllFreeIpBlocks(NetworkEntity entity);

   List<IpBlockEntity> findAllAssignedIpBlocks(NetworkEntity entity);

   List<IpBlockEntity> findAllAssignedIpBlocks(NetworkEntity entity,
         Long clusterId);

   void addIpBlocks(NetworkEntity entity, List<IpBlockEntity> ipBlocks);

   void removeIpBlocks(NetworkEntity entity, List<IpBlockEntity> ipBlocks);

   /**
    * Allocate IP addresses, the caller must not modify the contents in the
    * returned list.
    *
    * @param clusterId
    * @param count
    * @return
    */
   List<IpBlockEntity> alloc(NetworkEntity entity, long clusterId, long count);

   void free(NetworkEntity entity, long clusterId, List<IpBlockEntity> ipBlocks);

   void free(NetworkEntity entity, long clusterId);

   void setDnsType(NetworkEntity entity, NetworkDnsType dnsType);

   void setIsGenerateHostname(NetworkEntity entity, Boolean isGenerateHostname);
}
