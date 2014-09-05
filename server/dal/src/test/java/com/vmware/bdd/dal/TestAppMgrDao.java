/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.dal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;

import com.vmware.bdd.entity.AppManagerEntity;

/**
 * Created By xiaoliangl on 9/3/14.
 */
@ContextConfiguration(locations = {"classpath:/META-INF/spring/*-context.xml"})
public class TestAppMgrDao extends AbstractTransactionalTestNGSpringContextTests {
   @Autowired
   IAppManagerDAO appManagerDAO;

   @Test(expectedExceptions = DataIntegrityViolationException.class)
   @Transactional(propagation = Propagation.NEVER) //simulate two concurrent TXs: one user is inserting a APPManager, which another is doing the same.
   public void testPrimaryKeyViolation() {
      AppManagerEntity appManagerAddDefault = new AppManagerEntity();
      appManagerAddDefault.setName("fooAppMgr");
      appManagerAddDefault.setDescription("fooAppMgr");
      appManagerAddDefault.setType("fooAppMgr");
      appManagerAddDefault.setUrl("");
      appManagerAddDefault.setUsername("");
      appManagerAddDefault.setPassword("");
      appManagerAddDefault.setSslCertificate("");

      //TX1
      appManagerDAO.insert(appManagerAddDefault);

      //TX2
      appManagerDAO.insert(appManagerAddDefault);
   }

}
