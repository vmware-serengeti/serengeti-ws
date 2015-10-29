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

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.entity.RackEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/28/13
 * Time: 10:19 AM
 */
public class TestRackDAO {


   private ApplicationContext ctx;
   private IRackDAO rackDAO;

   @BeforeClass
   public void setup() {
      ctx = new ClassPathXmlApplicationContext("/META-INF/spring/*-context.xml");
      rackDAO = ctx.getBean(IRackDAO.class);
   }

   @Test
   public void testAddRack() {
      List<String> hosts = new ArrayList<String>();
      hosts.add("host1");
      hosts.add("host2");
      hosts.add("host3");
      rackDAO.addRack("rack1", hosts);
      List<RackEntity> racks = rackDAO.findAll();
      Assert.assertTrue(racks.size() == 1 && racks.get(0).getName().equals("rack1"));
   }

}
