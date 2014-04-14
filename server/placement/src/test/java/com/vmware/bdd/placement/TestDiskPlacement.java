/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.placement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.placement.PlacementPlanner;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.spectypes.DiskSpec;

/**
 * Test disk placement algorithms
 *
 * test various disk placement algorithms in PlacementPlanner, such as even
 * split, aggregate
 *
 * @author xiangfeiz
 *
 */
public class TestDiskPlacement {

   @Test
   public void testPlaceUnSeparableDisks() throws Exception {
      PlacementPlanner placementPlanner = new PlacementPlanner();

      List<DiskSpec> diskSpecs = new ArrayList<DiskSpec>();
      DiskSpec diskSpec = new DiskSpec();
      diskSpec.setName("diskSpec");
      diskSpec.setSize(40);
      diskSpecs.add(diskSpec);

      //5 Datastores with 5G, 10G, 20G, 30G, 50G free space
      List<AbstractDatastore> datastores = new ArrayList<AbstractDatastore>();
      AbstractDatastore datastore = new AbstractDatastore("datastore1", 5);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore2", 10);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore3", 20);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore4", 30);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore5", 50);
      datastores.add(datastore);

      Method placeUnSeparableDisks = PlacementPlanner.class.
            getDeclaredMethod("placeUnSeparableDisks", List.class, List.class);
      placeUnSeparableDisks.setAccessible(true);
      List<DiskSpec> placedDisks = (List<DiskSpec>) placeUnSeparableDisks.invoke(placementPlanner, diskSpecs, datastores);

      Assert.assertEquals(placedDisks.size(), 1);
      Assert.assertEquals(placedDisks.get(0).getTargetDs(), "datastore5");
      Assert.assertEquals(placedDisks.get(0).getSize(), 40);
   }

   @Test
   public void testEvenSpliter() throws Exception {
      PlacementPlanner placementPlanner = new PlacementPlanner();

      DiskSpec diskSpec = new DiskSpec();
      diskSpec.setName("diskSpec");
      diskSpec.setSize(50);

      //5 Datastores with 5G, 10G, 20G, 30G, 50G free space
      List<AbstractDatastore> datastores = new ArrayList<AbstractDatastore>();
      AbstractDatastore datastore = new AbstractDatastore("datastore1", 5);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore2", 10);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore3", 20);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore4", 30);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore5", 50);
      datastores.add(datastore);

      //Use reflection to invoke private method PlacementPlanner.evenSpliter()
      Method evenSpliter = PlacementPlanner.class.getDeclaredMethod("evenSpliter", DiskSpec.class, List.class);
      evenSpliter.setAccessible(true);
      List<DiskSpec> placedDisks = (List<DiskSpec>) evenSpliter.invoke(placementPlanner, diskSpec, datastores);

      StringBuffer output = new StringBuffer();
      for(DiskSpec disk : placedDisks) {
         output.append(disk.getTargetDs() + ":" + disk.getSize() + "G, ");
      }

      System.out.println(output.toString());
      Assert.assertEquals(output.toString(),
            "datastore1:5G, datastore2:10G, datastore3:12G, datastore4:12G, datastore5:11G, ");
   }

   @Test
   public void testAggregateSpliter() throws Exception {
      PlacementPlanner placementPlanner = new PlacementPlanner();

      DiskSpec diskSpec = new DiskSpec();
      diskSpec.setName("diskSpec");
      diskSpec.setSize(70);

      //5 Datastores with 5G, 10G, 20G, 30G, 50G free space
      List<AbstractDatastore> datastores = new ArrayList<AbstractDatastore>();
      AbstractDatastore datastore = new AbstractDatastore("datastore1", 5);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore2", 10);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore3", 20);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore4", 30);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore5", 50);
      datastores.add(datastore);

      //Use reflection to invoke private method PlacementPlanner.evenSpliter()
      Method aggregateSpliter = PlacementPlanner.class.getDeclaredMethod("aggregateSpliter", DiskSpec.class, List.class);
      aggregateSpliter.setAccessible(true);
      List<DiskSpec> placedDisks = (List<DiskSpec>) aggregateSpliter.invoke(placementPlanner, diskSpec, datastores);

      Assert.assertEquals(placedDisks.size(), 2);
      Assert.assertEquals(placedDisks.get(0).getTargetDs(), "datastore5");
      Assert.assertEquals(placedDisks.get(0).getSize(), 50);
      Assert.assertEquals(placedDisks.get(1).getTargetDs(), "datastore4");
      Assert.assertEquals(placedDisks.get(1).getSize(), 20);

   }

   @Test
   public void testEvenSpliterWithMinimumSpace() throws Exception {
      PlacementPlanner placementPlanner = new PlacementPlanner();

      DiskSpec diskSpec = new DiskSpec();
      diskSpec.setName("diskSpec");
      diskSpec.setSize(10);

      //5 Datastores with 5G, 10G, 20G, 30G, 50G free space
      List<AbstractDatastore> datastores = new ArrayList<AbstractDatastore>();
      AbstractDatastore datastore = new AbstractDatastore("datastore1", 10);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore2", 9);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore3", 8);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore4", 7);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore5", 3);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore6", 1);
      datastores.add(datastore);
      datastore = new AbstractDatastore("datastore7", 1);
      datastores.add(datastore);

      //Use reflection to invoke private method PlacementPlanner.evenSpliter()
      Method evenSpliter = PlacementPlanner.class.getDeclaredMethod("evenSpliter", DiskSpec.class, List.class);
      evenSpliter.setAccessible(true);
      List<DiskSpec> placedDisks = (List<DiskSpec>) evenSpliter.invoke(placementPlanner, diskSpec, datastores);

      StringBuffer output = new StringBuffer();
      for(DiskSpec disk : placedDisks) {
         output.append(disk.getTargetDs() + ":" + disk.getSize() + "G, ");
      }

      System.out.println(output.toString());
      Assert.assertEquals(output.toString(),
            "datastore5:2G, datastore4:2G, datastore3:2G, datastore2:2G, datastore1:2G, ");
   }
}
