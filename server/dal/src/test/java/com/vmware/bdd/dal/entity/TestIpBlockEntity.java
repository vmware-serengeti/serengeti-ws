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
package com.vmware.bdd.dal.entity;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.vmware.bdd.dal.IIpBlockDAO;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.utils.AuAssert;

public class TestIpBlockEntity {
   private static final Logger logger = Logger.getLogger(TestIpBlockEntity.class);

   IIpBlockDAO ipBlockDao;

   ApplicationContext ctx;

   @BeforeMethod
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("META-INF/spring/*-context.xml");
      ipBlockDao = ctx.getBean(IIpBlockDAO.class);
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

   private List<IpBlockEntity> genRandBlocks(int count) {
      List<IpBlockEntity> blocks = new ArrayList<IpBlockEntity>(count);
      NetworkEntity network = new NetworkEntity("net1", "vmnet1", AllocType.IP_POOL,
            "255.255.255.0", "192.168.1.1", "8.8.8.8", null);

      Long[] owners = { 1L, 2L, 3L, 4L, 5L };
      BlockType[] blockTypes = { BlockType.ASSIGNED, BlockType.FREE };

      for (int i = 0; i < count; ++i) {
         Long ownerId = IpBlockEntity.FREE_BLOCK_OWNER_ID;
         BlockType blockType = blockTypes[(int) (Math.random() * blockTypes.length)
                                % blockTypes.length];
         if (blockType != BlockType.FREE) {
            ownerId = owners[(int) (Math.random() * owners.length) % owners.length];
         }

         int maxValue = count * 10;
         Long end = 1 + (long) (Math.random() * maxValue) % maxValue;
         Long begin = (long) (Math.random() * end) % end;

         blocks.add(new IpBlockEntity(network, ownerId, blockType, begin, end));
      }
      return blocks;
   }

   /**
    * Split the block randomly, the original block is kept unchanged.
    * 
    * @param block
    *           original block
    * @return the split block(s)
    */
   private List<IpBlockEntity> randSplit(IpBlockEntity block) {
      List<IpBlockEntity> split = new ArrayList<IpBlockEntity>(2);

      if (block.getLength() > 1 && Math.random() < 0.5) {
         Long splitIp = block.getBeginIp() +
               (long) (Math.random() * (block.getLength() - 1))
               % (block.getLength() - 1);

         IpBlockEntity left = new IpBlockEntity(block.getNetwork(), block.getOwnerId(),
               block.getType(),
               block.getBeginIp(),
               splitIp);

         IpBlockEntity right = new IpBlockEntity(block.getNetwork(), block.getOwnerId(),
               block.getType(),
               splitIp + 1,
               block.getEndIp());

         split.add(left);
         split.add(right);
      } else {
         IpBlockEntity dup = new IpBlockEntity(block.getNetwork(), block.getOwnerId(),
               block.getType(), block.getBeginIp(), block.getEndIp());
         split.add(dup);
      }

      return split;
   }

   /**
    * Randomly expand a block in-place.
    * 
    * @return expanded block
    */
   private IpBlockEntity randExpand(IpBlockEntity block, long min, long max) {
      if (Math.random() < 0.2) {
         Long begin = block.getBeginIp();
         Long end = block.getEndIp();

         AuAssert.check(begin >= min && end <= max);
         begin = Math.max(min, begin - (long) (Math.random() * 3));
         end = Math.min(max, end + (long) (Math.random() * 3));

         block.setBeginIp(begin);
         block.setEndIp(end);
      }

      return block;
   }

   /**
    * Random shuffle the blocks and randomly group them as separated groups.
    */
   private List<List<IpBlockEntity>> randShuffleAndGroup(List<IpBlockEntity> blocks) {
      Collections.shuffle(blocks);
      List<List<IpBlockEntity>> result = new ArrayList<List<IpBlockEntity>>();

      Iterator<IpBlockEntity> iter = blocks.iterator();
      while (iter.hasNext()) {
         List<IpBlockEntity> subGroup = new ArrayList<IpBlockEntity>();
         subGroup.add(iter.next());
         while (Math.random() < 0.9 && iter.hasNext()) {
            subGroup.add(iter.next());
         }
         result.add(subGroup);
      }

      return result;
   }

   /**
    * Split a block randomly into optionally overlapped pieces and randomly
    * group them into several groups.
    * 
    * @param original
    *           original block
    * @param splitLevel
    *           split level
    * @param allowOverlap
    *           whether allow overlapping pieces
    * @return result set
    */
   private List<List<IpBlockEntity>> torn(IpBlockEntity original, int splitLevel,
         boolean allowOverlap) {
      List<IpBlockEntity> split = new ArrayList<IpBlockEntity>();
      split.add(original);
      for (int i = 0; i < splitLevel; ++i) {
         List<IpBlockEntity> base = split;
         split = new ArrayList<IpBlockEntity>();
         for (IpBlockEntity blk : base) {
            split.addAll(randSplit(blk));
         }
      }

      if (allowOverlap) {
         for (IpBlockEntity blk : split) {
            randExpand(blk, original.getBeginIp(), original.getEndIp());
         }
      }

      return randShuffleAndGroup(split);
   }

   @Test
   public void testCompareTo() {
      List<IpBlockEntity> blocks = genRandBlocks(100);
      for (IpBlockEntity b1 : blocks) {
         for (IpBlockEntity b2 : blocks) {
            // some redundancies
            assertTrue(b1.compareTo(b2) == 0 && b2.compareTo(b1) == 0 ||
                  b1.compareTo(b2) > 0 && b2.compareTo(b1) < 0 ||
                  b1.compareTo(b2) < 0 && b2.compareTo(b1) > 0);
         }
      }
   }

   @Test
   public void testContainAndOverlap() {
      int N = 100;
      int count = 0;

      List<IpBlockEntity> blocks = genRandBlocks(N);
      for (IpBlockEntity b1 : blocks) {
         for (IpBlockEntity b2 : blocks) {
            if (b1.contains(b2)) {
               ++count;
            }

            assertTrue(!b1.contains(b2) ||
                  b1.contains(b2) && b1.isOverlapedWith(b2) && b2.isOverlapedWith(b1));
            assertTrue(!b1.isOverlapedWith(b2) ||
                  b1.isOverlapedWith(b2) && b2.isOverlapedWith(b1));
         }
      }

      assertTrue(count >= N);
   }

   @Test
   public void testMergeBasic() {
      NetworkEntity network = new NetworkEntity("net1", "vmnet1", AllocType.IP_POOL,
            "255.255.255.0", "192.168.1.1", "8.8.8.8", null);
      IpBlockEntity blk1 = new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 1L, 3L);
      IpBlockEntity blk2 = new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 4L, 6L);
      IpBlockEntity blk3 = new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 5L, 10L);

      List<IpBlockEntity> split = new ArrayList<IpBlockEntity>();
      split.add(blk1);
      split.add(blk2);
      split.add(blk3);
      logger.info("split: " + collectionToString(split));
      List<IpBlockEntity> merged = ipBlockDao.merge(split, false, false, true);
      logger.info("merged: " + collectionToString(merged));

      assertEquals(1, merged.size());
      assertEquals(10, merged.get(0).getLength());
   }

   /**
    * Randomly torn a whole IP blocks into optionally overlapped pieces and then
    * shuffle and group the pieces into some random groups. Then merge these
    * groups and join them in a single list and make a final merge. The finally
    * merged list should match to the original one.
    */
   @Test(enabled=false)
   private void doMergeRandomTest(boolean allowOverlap) {
      IpBlockEntity original = new IpBlockEntity(new NetworkEntity("net1", "vmnet1",
            AllocType.IP_POOL, "255.255.255.0", "192.168.1.1", "8.8.8.8", null), 1L,
            BlockType.ASSIGNED, 1L, 1L << 17/* 131072 */);

      List<IpBlockEntity> allBlocks = new ArrayList<IpBlockEntity>();
      for (List<IpBlockEntity> grp : torn(original, 20, allowOverlap)) {
         allBlocks.addAll(ipBlockDao.merge(grp, false, false, allowOverlap));
      }

      List<IpBlockEntity> merged =
         ipBlockDao.merge(allBlocks, false, false, allowOverlap);
      logger.info("merged: " + collectionToString(merged));

      assertEquals(1, merged.size());
      assertEquals(original, merged.get(0));
   }

   @Test
   public void testMergeRandom() {
      doMergeRandomTest(true);
      doMergeRandomTest(false);
   }

   @Test
   public void testSubtractBasic() {
      NetworkEntity network = new NetworkEntity("net1", "vmnet1", AllocType.IP_POOL,
            "255.255.255.0", "192.168.1.1", "8.8.8.8", null);;

      List<IpBlockEntity> setA = new ArrayList<IpBlockEntity>();
      setA.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 1L, 10L));

      List<IpBlockEntity> setB1 = new ArrayList<IpBlockEntity>();
      setB1.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 1L, 3L));
      List<IpBlockEntity> setB2 = new ArrayList<IpBlockEntity>();
      setB2.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 4L, 6L));
      List<IpBlockEntity> setB3 = new ArrayList<IpBlockEntity>();
      setB3.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 5L, 10L));

      List<IpBlockEntity> setDiff = setA;
      setDiff = IpBlockEntity.subtract(setDiff, setB1);
      setDiff = IpBlockEntity.subtract(setDiff, setB2);
      setDiff = IpBlockEntity.subtract(setDiff, setB3);

      assertTrue(setDiff.isEmpty());

      List<IpBlockEntity> setC = new ArrayList<IpBlockEntity>();
      setC.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 3337306476L, 3337306480L));
      List<IpBlockEntity> setD = new ArrayList<IpBlockEntity>();
      setD.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 3337306476L, 3337306480L));
      setD.add(new IpBlockEntity(network, 1L, BlockType.ASSIGNED, 3337306476L, 3337306476L));
      setDiff = setC;
      setD = ipBlockDao.merge(setD, true, true, true);
      setDiff = IpBlockEntity.subtract(setDiff, setD);

      assertTrue(setDiff.isEmpty());

      setDiff = setC;
      setD = ipBlockDao.merge(setD, true, true, true);
      setDiff = IpBlockEntity.subtract(setD, setDiff);

      assertTrue(setDiff.isEmpty());
   }

   /**
    * Randomly torn a whole IP blocks into optionally overlapped and shuffled
    * pieces and then subtract the original block with all these pieces one by
    * one. Finally the result diff will equals to en empty set.
    */
   @Test(enabled=false)
   private void doSubtractRandomTest(boolean allowOverlap) {
      IpBlockEntity original = new IpBlockEntity(new NetworkEntity("net1", "vmnet1",
            AllocType.IP_POOL, "255.255.255.0", "192.168.1.1", "8.8.8.8", null), 1L,
            BlockType.ASSIGNED, 1L, 1L << 15);

      List<IpBlockEntity> setA = new ArrayList<IpBlockEntity>();
      setA.add(original);
      List<IpBlockEntity> setDiff = setA;

      for (List<IpBlockEntity> grp : torn(original, 18, allowOverlap)) {
         setDiff = IpBlockEntity.subtract(setDiff,
               ipBlockDao.merge(grp, false, false, allowOverlap));
         logger.debug("#diff: " + IpBlockEntity.count(setDiff));
         logger.debug("diff: " + collectionToString(setDiff));
      }

      assertTrue(setDiff.isEmpty());
   }

   @Test
   public void testSubtractRandom() {
      doSubtractRandomTest(true);
      doSubtractRandomTest(false);
   }
}
