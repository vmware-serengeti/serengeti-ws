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
package com.vmware.bdd.dal;

import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.entity.CloudProviderConfigEntity;
import com.vmware.bdd.entity.Saveable;

public class TestDAL {

   @BeforeMethod
   public void setup() {

   }

   @AfterMethod
   public void tearDown() {

   }

   @AfterClass
   public static void deleteAll() {
      final List<CloudProviderConfigEntity> attrs =
            CloudProviderConfigEntity.findAllByType("VC");
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            for (CloudProviderConfigEntity attr : attrs) {
               attr.delete();
            }
            return null;
         }
      });
   }

   @Test(groups = {"testDAL"})
   public void testIntransInsert() {
      DAL.inRwTransactionDo(new Saveable<CloudProviderConfigEntity>() {
         public CloudProviderConfigEntity body() {
            CloudProviderConfigEntity vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_addr");
            vcCloud.setValue("10.1.170.1");
            DAL.insert(vcCloud);
            return vcCloud;
         }
      });

      List<CloudProviderConfigEntity> attrs =
            CloudProviderConfigEntity.findAllByType("VC");
      assertTrue(
            "Excepted 1 or more attributes, but get " + attrs.size(),
            attrs.size() >= 1);
   }

   @Test(groups = {"testDAL"}, dependsOnMethods = { "testIntransInsert" })
   public void testIntransDelete() {
      List<CloudProviderConfigEntity> attrs =
            CloudProviderConfigEntity.findAllByType("VC");
      assertTrue("Excepted 1 attributes, but get " + attrs.size(),
            attrs.size() == 1);
      DAL.inTransactionDelete(attrs.get(0));

      attrs = CloudProviderConfigEntity.findAllByType("VC");
      assertTrue("Excepted 2 attributes, but get " + attrs.size(),
            attrs.size() == 0);
   }

   @Test(enabled=false)
   public void testInRotransDo() {
      DAL.inRoTransactionDo(new Saveable<Void>() {
         public Void body() {
            CloudProviderConfigEntity vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_addr");
            vcCloud.setValue("10.1.170.1");
            try {
               vcCloud.insert();
            } catch (Exception e) {
               assertTrue("Got excepted exception " + e, true);
               return null;
            }
            assertTrue("Does not catch expected exception,", false);
            return null;
         }
      });
   }

   @Test(groups = {"testDAL"}, dependsOnMethods = { "testIntransDelete" })
   public void testInRwtransDo() {
      DAL.inRwTransactionDo(new Saveable<Void>() {
         public Void body() {
            CloudProviderConfigEntity vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_addr");
            vcCloud.setValue("10.1.170.1");
            vcCloud.insert();
            return null;
         }
      });
      List<CloudProviderConfigEntity> attrs =
            CloudProviderConfigEntity.findAllByType("VC");
      assertTrue("Excepted 1 attributes, but get " + attrs.size(),
            attrs.size() == 1);
      DAL.inTransactionDelete(attrs.get(0));
   }

   private void prepareData() {
       DAL.inRwTransactionDo(new Saveable<Void>() {
         public Void body() {
            CloudProviderConfigEntity vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_addr");
            vcCloud.setValue("10.1.170.1");
            DAL.insert(vcCloud);

            vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_user");
            vcCloud.setValue("line");
            DAL.insert(vcCloud);

            vcCloud = new CloudProviderConfigEntity();
            vcCloud.setCloudType("VC");
            vcCloud.setAttribute("vc_template");
            vcCloud.setValue("centos-5.3");
            DAL.insert(vcCloud);
            return null;
         }
      });
   }

   @Test(groups = {"testDAL"}, dependsOnMethods = { "testInRwtransDo" })
   public void testFind() {
      prepareData();
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            List<CloudProviderConfigEntity> attrs =
                  DAL.findAll(CloudProviderConfigEntity.class);
            assertTrue("Excepted 3 attributes, but get " + attrs.size(),
                  attrs.size() == 3);

            attrs =
                  DAL.findByCriteria(CloudProviderConfigEntity.class,
                        Restrictions.eq("cloudType", "VC"));
            assertTrue("Excepted 3 attributes, but get " + attrs.size(),
                  attrs.size() == 3);

            assertTrue("Should be in transaction.",
                  DAL.isInTransaction());
            return null;
         }
      });
   }
}
