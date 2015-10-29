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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.IpAddressUtil;

public class TestNetworkDAO {
   private static final Logger logger = Logger.getLogger(TestNetworkDAO.class);

   ApplicationContext ctx;
   private INetworkDAO networkDao;

   @BeforeMethod
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("META-INF/spring/*-context.xml");
      networkDao = ctx.getBean(INetworkDAO.class);
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

   public static void main(String[] args) throws UnknownHostException {
      TestNetworkDAO test = new TestNetworkDAO();
      test.setup();
      test.testInsert();
   }

   @Test(groups = { "testNetworkEntity" })
   public void testInsert() throws UnknownHostException {
      final String portGroup = "VM Network " + Math.random();
      final AllocType allocType = AllocType.IP_POOL;
      final String netmask = "255.255.255.0";
      final String gateway = "192.168.1.254";
      final String dns1 = "8.8.8.8";
      final String dns2 = "4.4.4.4";

      NetworkEntity network = new NetworkEntity(portGroup, portGroup, allocType,
            netmask, gateway, dns1, dns2, NetworkDnsType.NORMAL);
      networkDao.insert(network);
      Long ipBase = IpAddressUtil.getAddressAsLong(InetAddress.getByName("192.168.1.1"));
      List<IpBlockEntity> ipBlocks = new ArrayList<IpBlockEntity>();
      for (int i = 0; i < 10; i++) {
         ipBlocks.add(new IpBlockEntity(network, IpBlockEntity.FREE_BLOCK_OWNER_ID,
               BlockType.FREE, ipBase + i * 10, ipBase + i * 10 + 10));
      }

      networkDao.addIpBlocks(network, ipBlocks);
      logger.info(network);

      NetworkEntity network2 = networkDao.findById(network.getId());
      assertNotNull(network2);

      NetworkEntity net = networkDao.findById(network.getId());

      networkDao.delete(net);

      assertNull(networkDao.findById(network.getId()));

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
   @Test(groups = { "testNetworkEntity" }, dependsOnMethods = { "testInsert" })
   public void testAllocFreeRandom() throws UnknownHostException {
      final String portGroup = "VM Network " + Math.random();
      final AllocType allocType = AllocType.IP_POOL;
      final String netmask = "255.0.0.0";
      final String gateway = "0.0.0.1";
      final String dns1 = "8.8.8.8";
      final String dns2 = null;

      final Long beginIp = IpAddressUtil.getAddressAsLong(InetAddress
            .getByName("0.0.0.2"));
      final Long endIp = IpAddressUtil
            .getAddressAsLong(InetAddress.getByName("0.2.0.0"));

      NetworkEntity network = new NetworkEntity(portGroup, portGroup, allocType,
            netmask, gateway, dns1, dns2, NetworkDnsType.NORMAL);
      IpBlockEntity originalBlock = new IpBlockEntity(network,
            IpBlockEntity.FREE_BLOCK_OWNER_ID, BlockType.FREE, beginIp, endIp);
      networkDao.insert(network);

      List<IpBlockEntity> ipBlocks = new ArrayList<IpBlockEntity>();
      ipBlocks.add(originalBlock);

      networkDao.addIpBlocks(network, ipBlocks);
      logger.info(network);

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

            try {
               List<IpBlockEntity> allocated = networkDao.alloc(network, ownerId, count);
               allocLists.add(allocated);
               allocSizes.add(count);
               allocOwners.add(ownerId);
               totalAssigned += totalAssigned + count;
               logger.debug("alloc: "
                     + collectionToString(allocLists.get(allocLists.size() - 1)));
            } catch (NetworkException ex) {
               assertTrue("out of ip resource", network.getFree() < count);
               break;
            }
         } else { // free
            if (!allocLists.isEmpty()) {
               int idx = (int) (Math.random() * allocLists.size()) % allocLists.size();
               final List<IpBlockEntity> toBeFreed = allocLists.remove(idx);
               final Long ownerId = allocOwners.remove(idx);

               logger.debug("to free: " + collectionToString(toBeFreed));
               networkDao.free(network, ownerId, toBeFreed);

               totalAssigned -= allocSizes.remove(idx);
            }
         }
      }

      // free all by cluster
      for (long id = 0; id < maxOwnerId; ++id) {
         final Long ownerId = id;
         networkDao.free(network, ownerId);
      }

      originalBlock = new IpBlockEntity(network, IpBlockEntity.FREE_BLOCK_OWNER_ID,
            BlockType.FREE, beginIp, endIp);
      assertEquals(1, network.getIpBlocks().size());
      assertEquals(originalBlock, network.getIpBlocks().get(0));

      NetworkEntity net = networkDao.findById(network.getId());

      networkDao.delete(net);
      logger.info("alloc/free test done");
   }
}
