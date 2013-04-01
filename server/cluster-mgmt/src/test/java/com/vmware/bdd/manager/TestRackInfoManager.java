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
package com.vmware.bdd.manager;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.RackInfo;

@ContextConfiguration(locations = { "classpath:/spring/*-context.xml" })
public class TestRackInfoManager extends
      AbstractTransactionalTestNGSpringContextTests {
   @Autowired
   RackInfoManager rim;

   @BeforeMethod
   public void setup() {

   }

   @AfterMethod
   public void tearDown() {

   }

   @Test
   public void testInsert() {
      List<RackInfo> racksInfo = new ArrayList<RackInfo>();
      List<String> hosts1 = new ArrayList<String>();
      hosts1.add("host1-1");
      hosts1.add("task1-2");
      List<String> hosts2 = new ArrayList<String>();
      hosts2.add("host2-1");
      hosts2.add("task2-2");
      RackInfo rackInfo1 = new RackInfo();
      RackInfo rackInfo2 = new RackInfo();
      rackInfo1.setName("rack1");
      rackInfo1.setHosts(hosts1);
      rackInfo2.setName("rack2");
      rackInfo2.setHosts(hosts2);
      racksInfo.add(rackInfo1);
      racksInfo.add(rackInfo2);

      rim.importRackInfo(racksInfo);
      assertEquals(racksInfo.get(0).getName(), rim.exportRackInfo().get(0)
            .getName());
      rim.importRackInfo(racksInfo);
      assertEquals(racksInfo.get(0).getName(), rim.exportRackInfo().get(0)
            .getName());
   }
}
