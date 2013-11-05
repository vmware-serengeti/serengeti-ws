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
package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.List;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.utils.IpAddressUtil;

public class NetworkServiceTest {
   @Mocked
   private INetworkDAO networkDao;
   @Mocked
   private IResourceService resService;
   @Mocked
   private IClusterDAO clusterDAO;

   private NetworkService networkSvc;

   private List<NetworkEntity> networks;
   private NetworkEntity entity;
   private NetworkEntity tempEntity;

   @BeforeClass
   public void beforeClass() {
      networkSvc = new NetworkService();
      networks = new ArrayList<NetworkEntity>();
      entity = new NetworkEntity();
      entity.setName("defaultNet");
      entity.setAllocType(AllocType.DHCP);
      entity.setPortGroup("network");
      networks.add(entity);
   }

   @Test(groups = { "res-mgmt" })
   public void addDhcpNetwork() {
      new Expectations() {
         {
            resService.isNetworkExistInVc(anyString);
            result = true;
         }
      };
      networkSvc.setResService(resService);
      networkSvc.setNetworkDao(networkDao);
      networkSvc.addDhcpNetwork("defaultNetwork", "network1");
      new Verifications() {
         {
            networkDao.insert(withAny(new NetworkEntity()));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void addIpPoolNetwork() {
      new Expectations() {
         {
            resService.isNetworkExistInVc(anyString);
            result = true;
         }
      };
      networkSvc.setResService(resService);
      networkSvc.setNetworkDao(networkDao);
      List<IpBlock> ipBlocks = new ArrayList<IpBlock>();
      ipBlocks.add(new IpBlock("192.168.1.1", "192.168.1.10"));
      networkSvc.addIpPoolNetwork("staticNetwork", "network2", "255.255.255.0",
            "192.168.1.254", "8.8.8.8", "4.4.4.4", ipBlocks);

      new Verifications() {
         {
            NetworkEntity networkEntity = new NetworkEntity();
            networkDao.insert(withAny(networkEntity));
            networkDao.addIpBlocks(withAny(networkEntity),
                  withAny(new ArrayList<IpBlockEntity>()));
         }
      };
   }

   @Test(groups = { "res-mgmt" })
   public void deleteNetwork() {
      new Expectations() {
         {
            resService.isNetworkExistInVc(anyString);
            result = true;
         }
      };
      networkSvc.setResService(resService);
      networkSvc.setNetworkDao(networkDao);
      networkSvc.setClusterDAO(clusterDAO);
      networkSvc.addDhcpNetwork("defaultNetwork3", "network3");
      new Verifications() {
         {
            networkDao.insert(withAny(new NetworkEntity()));
         }
      };

      new Expectations() {
         {
            NetworkEntity network = new NetworkEntity();
            network.setIpBlocks(new ArrayList<IpBlockEntity>());
            networkDao.findNetworkByName("defaultNetwork3");
            result = network;
            networkDao.delete(withAny(network));
         }
      };
      networkSvc.removeNetwork("defaultNetwork3");
   }

   @Test(groups = { "res-mgmt" })
   public void getAllNetworkEntities() {
      new Expectations() {
         {
            networkDao.findAllNetworks();
            result = networks;
         }
      };
      networkSvc.setNetworkDao(networkDao);
      List<NetworkEntity> networks = networkSvc.getAllNetworkEntities();
      Assert.assertNotNull(networks);
   }

   @Test(groups = { "res-mgmt" })
   public void getAllNetworks() {
      new Expectations() {
         {
            networkDao.findAllNetworks();
            result = networks;
         }
      };
      networkSvc.setNetworkDao(networkDao);
      List<NetworkRead> networks = networkSvc.getAllNetworks(false);
      Assert.assertNotNull(networks);
      Assert.assertEquals(networks.size(), 1);
   }

   @Test(groups = { "res-mgmt" })
   public void increaseIPs() {
      final NetworkEntity network = new NetworkEntity();
      network.setName("staticNetwork");
      network.setPortGroup("portGroup1");
      network.setAllocType(AllocType.IP_POOL);
      network.setNetmask("255.255.255.0");
      network.setGateway("192.168.1.1");
      network.setDns1("10.1.1.2");
      network.setDns2("10.1.1.3");
      List<IpBlock> ipBlocks = new ArrayList<IpBlock>();
      ipBlocks.add(new IpBlock("192.168.1.11", "192.168.1.12"));
      final List<IpBlockEntity> blocks =
            new ArrayList<IpBlockEntity>(ipBlocks.size());
      for (IpBlock ib : ipBlocks) {
         IpBlockEntity blk =
               new IpBlockEntity(network, IpBlockEntity.FREE_BLOCK_OWNER_ID,
                     BlockType.FREE, IpAddressUtil.getAddressAsLong(ib
                           .getBeginIp()), IpAddressUtil.getAddressAsLong(ib
                           .getEndIp()));
         blocks.add(blk);
      }
      network.setIpBlocks(blocks);
      networkSvc.setNetworkDao(networkDao);
      new Expectations() {
         {
            networkSvc.getNetworkEntityByName(anyString);
            result = network;
         }
         {
            networkDao.addIpBlocks(network, network.getIpBlocks());
         }
      };
      networkSvc.increaseIPs("staticNetwork", ipBlocks);
   }

   @Test(groups = { "res-mgmt" })
   public void testFree() {
      networkSvc.setNetworkDao(networkDao);
      networkSvc.free(entity, 1L, 123456789);
      new Verifications() {
         {
            networkDao.free(withAny(entity), anyLong, withAny(new ArrayList<IpBlockEntity>()));
         }
      };
   }
}
