/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.IpAddressUtil;

public class TestNetworkEntity {
   private static final Logger logger = Logger.getLogger(TestNetworkEntity.class);

   @BeforeMethod
   public void setup() {

   }

   @AfterMethod
   public void tearDown() {

   }

   @AfterClass
   public static void deleteAll() {
   }

   private String collectionToString(Collection<?> c) {
      String str = "";
      for (Object o : c) {
         str += "\n" + o;
      }
      return str;
   }

   @Test(groups = {"testNetworkEntity"})
   public void testInsert() throws UnknownHostException {
      final String portGroup = "VM Network " + Math.random();
      final AllocType allocType = AllocType.IP_POOL;
      final String netmask = "255.255.255.0";
      final String gateway = "192.168.1.254";
      final String dns1 = "8.8.8.8";
      final String dns2 = "4.4.4.4";

      final NetworkEntity network = DAL.inTransactionDo(new Saveable<NetworkEntity>() {
         public NetworkEntity body() throws UnknownHostException {
            NetworkEntity network = new NetworkEntity(portGroup, portGroup, allocType,
                  netmask, gateway, dns1, dns2);
            network.insert();
            Long ipBase = IpAddressUtil.getAddressAsLong(InetAddress
                  .getByName("192.168.1.1"));
            List<IpBlockEntity> ipBlocks = new ArrayList<IpBlockEntity>();
            for (int i = 0; i < 10; i++) {
               ipBlocks.add(new IpBlockEntity(network,
                     IpBlockEntity.FREE_BLOCK_OWNER_ID, BlockType.FREE, ipBase + i * 10,
                     ipBase + i * 10 + 10));
            }

            network.addIpBlocks(ipBlocks);
            logger.info(network);
            return network;
         }
      });

      NetworkEntity network2 = DAL.inTransactionDo(new Saveable<NetworkEntity>() {
         public NetworkEntity body() throws UnknownHostException {
            return DAL.findById(NetworkEntity.class, network.getId());
         }
      });

      assertNotNull(network2);

      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            NetworkEntity net = DAL.findById(NetworkEntity.class, network.getId());

            for (IpBlockEntity blk : net.getIpBlocks()) {
               blk.delete();
            }

            net.delete();
            return null;
         }
      });

      NetworkEntity network3 = DAL.inTransactionDo(new Saveable<NetworkEntity>() {
         public NetworkEntity body() throws UnknownHostException {
            return DAL.findById(NetworkEntity.class, network.getId());
         }
      });

      assertNull(network3);
   }

   /**
    * First initialize the network with a whole IP block. And randomly do alloc
    * and free request until an 'out of IP' exception thrown (P(alloc) >
    * P(free), then the process will eventually converge). And then free all the
    * allocated IPs for all the requesters.
    * 
    * The expected result is: the final IP blocks in the table should have only
    * one b block which match the original one.
    * 
    * @throws UnknownHostException
    */
   @Test(groups = {"testNetworkEntity"}, dependsOnMethods = { "testInsert" })
   public void testAllocFreeRandom() throws UnknownHostException {
      final String portGroup = "VM Network " + Math.random();
      final AllocType allocType = AllocType.IP_POOL;
      final String netmask = "255.0.0.0";
      final String gateway = "0.0.0.1";
      final String dns1 = "8.8.8.8";
      final String dns2 = null;

      final Long beginIp = IpAddressUtil.getAddressAsLong(InetAddress
            .getByName("0.0.0.2"));
      final Long endIp = IpAddressUtil.getAddressAsLong(InetAddress
            .getByName("0.2.0.0"));

      final NetworkEntity network = DAL.inTransactionDo(new Saveable<NetworkEntity>() {
         public NetworkEntity body() throws UnknownHostException {
            NetworkEntity network = new NetworkEntity(portGroup, portGroup, allocType,
                  netmask, gateway, dns1, dns2);
            IpBlockEntity originalBlock = new IpBlockEntity(network,
                  IpBlockEntity.FREE_BLOCK_OWNER_ID,
                  BlockType.FREE, beginIp, endIp);
            network.insert();

            List<IpBlockEntity> ipBlocks = new ArrayList<IpBlockEntity>();
            ipBlocks.add(originalBlock);

            network.addIpBlocks(ipBlocks);
            logger.info(network);
            return network;
         }
      });

      IpBlockEntity originalBlock = network.getIpBlocks().get(0);

      List<List<IpBlockEntity>> allocLists = new ArrayList<List<IpBlockEntity>>();
      List<Integer> allocSizes = new ArrayList<Integer>();
      List<Long> allocOwners = new ArrayList<Long>();
      long totalAssigned = 0L;

      Long maxOwnerId = 100L; // min is 0L
      int minAllocSize = 1;
      int maxAllocSize = 1000;

      final Long totalIps = endIp - beginIp + 1;

      long nextLogTimeMs = System.currentTimeMillis() + 1000;

      logger.info("strart random alloc/free stress test");
      /**
       * try to alloc until out of IPs
       */
      while (true) {
         long now = System.currentTimeMillis();
         if (nextLogTimeMs < now) {
            nextLogTimeMs = now + 1000;
            logger.info("allocated: " + totalAssigned + "/" + totalIps + " ("
                  + totalAssigned / (double) totalIps + ")");
         }

         if (Math.random() < 0.8) { // alloc
            final Long ownerId = (long) (Math.random() * maxOwnerId) % maxOwnerId;
            int rndCount = (int) (Math.random() * maxAllocSize) % maxAllocSize;
            final int count = Math.max(minAllocSize, rndCount);

            List<IpBlockEntity> allocated = DAL
                  .inTransactionDo(new Saveable<List<IpBlockEntity>>() {
                     public List<IpBlockEntity> body() throws UnknownHostException {
                        DAL.refresh(network);
                        try {
                           return network.alloc(ownerId, count);
                        } catch (NetworkException ex) {
                           assertTrue("out of ip resource",
                                 network.getFree() < count);
                           return null;
                        }
                     }
                  });

            if (allocated == null) {
               break;
            } else {
               allocLists.add(allocated);
               allocSizes.add(count);
               allocOwners.add(ownerId);
               totalAssigned += totalAssigned + count;
               logger.debug("alloc: " +
                     collectionToString(allocLists.get(allocLists.size() - 1)));
            }
         } else { // free
            if (!allocLists.isEmpty()) {
               int idx = (int) (Math.random() * allocLists.size()) % allocLists.size();
               final List<IpBlockEntity> toBeFreed = allocLists.remove(idx);
               final Long ownerId = allocOwners.remove(idx);

               DAL.inTransactionDo(new Saveable<Void>() {
                  public Void body() throws UnknownHostException {
                     DAL.refresh(network);
                     logger.debug("to free: " + collectionToString(toBeFreed));
                     network.free(ownerId, toBeFreed);
                     logger.debug("after free: "
                           + collectionToString(network.getIpBlocks()));
                     return null;
                  }
               });

               totalAssigned -= allocSizes.remove(idx);
            }
         }
      }

      // free all by cluster
      for (long id = 0; id < maxOwnerId; ++id) {
         final Long ownerId = id;
         DAL.inTransactionDo(new Saveable<Void>() {
            public Void body() throws UnknownHostException {
               DAL.refresh(network);
               network.free(ownerId);
               return null;
            }
         });
      }

      assertEquals(1, network.getIpBlocks().size());
      assertEquals(originalBlock, network.getIpBlocks().get(0));

      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            NetworkEntity net = DAL.findById(NetworkEntity.class, network.getId());

            for (IpBlockEntity blk : net.getIpBlocks()) {
               blk.delete();
            }

            net.delete();
            return null;
         }
      });
      logger.info("alloc/free test done");
   }
}
