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
package com.vmware.bdd.service.resmgmt;

import java.util.List;

import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.NetworkEntity;

public interface INetworkService {

   NetworkEntity addDhcpNetwork(final String name, final String portGroup);

   NetworkEntity addIpPoolNetwork(final String name, final String portGroup,
         final String netmask, final String gateway, final String dns1,
         final String dns2, final List<IpBlock> ipBlocks);

   NetworkEntity getNetworkEntityByName(final String name);

   NetworkRead getNetworkByName(final String name, final boolean withDetails);

   List<String> getPortGroupsByNames(final List<String> names);

   List<NetworkEntity> getAllNetworkEntities();

   List<NetworkRead> getAllNetworks(final boolean withDetails);

   void increaseIPs(String networkName, List<IpBlock> ipBlocks);

   void removeNetwork(NetworkEntity network);

   void removeNetwork(final String networkName);

   void addIpBlocks(NetworkEntity network, List<IpBlockEntity> toAdd);

   void removeIpBlocks(NetworkEntity network, List<IpBlockEntity> toRemove);

   /**
    * Allocate IP addresses from a network for a cluster.
    *
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @param count
    *           IP count
    * @return allocated IP addresses
    */
   List<IpBlockEntity> alloc(NetworkEntity network, long clusterId, long count);

   /**
    * Get all allocated IP blocks allocated from a specified network.
    *
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @return IP blocks list
    */
   List<IpBlockEntity> getAllocatedIpBlocks(NetworkEntity network,
         long clusterId);

   /**
    * Free the specified IP blocks, these IP blocks must be allocated from this
    * network.
    *
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @param ipBlocks
    *           IP blocks
    */
   void free(NetworkEntity network, long clusterId, List<IpBlockEntity> ipBlocks);

   /**
    * Try to free all the ip blocks of an cluster if any assigned.
    *
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    */
   void free(NetworkEntity network, long clusterId);

   /**
    * Try to free a single ip address.
    *
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @param ipAddr
    *           ip address to be freed
    */
   void free(NetworkEntity network, long clusterId, long ipAddr);

}