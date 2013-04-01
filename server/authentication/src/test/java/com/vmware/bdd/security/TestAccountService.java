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
package com.vmware.bdd.security;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.entity.User;
import com.vmware.bdd.entity.Users;
import com.vmware.bdd.security.service.impl.UserService;
import com.vmware.bdd.utils.FileUtils;
import com.vmware.bdd.utils.TestFileUtils;


public class TestAccountService {

   public static final String UsersFile = "Users.xml";

   @Test
   public void testLoadUserByUsername() throws JAXBException {
      UserDetails userDetails1 = null;
      UserDetailsService accountService = new UserService();
      Users users1 = new Users();
      User user1 = new User();
      user1.setName("serengeti");
      users1.setUsers(Arrays.asList(user1));
      TestFileUtils.createXMLFile(users1, FileUtils.getConfigFile(UsersFile, "Users"));
      try {
         userDetails1 = accountService.loadUserByUsername("root");
      } catch (UsernameNotFoundException e) {
      }
      Assert.assertNull(userDetails1);
      UserDetails userDetails2 = accountService.loadUserByUsername("serengeti");
      assertNotNull(userDetails2);
      TestFileUtils.deleteXMLFile(FileUtils.getConfigFile(UsersFile, "Users"));
      Users users2 = new Users();
      User user2 = new User();
      user2.setName("*");
      users2.setUsers(Arrays.asList(user2));
      TestFileUtils.createXMLFile(users2, FileUtils.getConfigFile(UsersFile, "Users"));
      userDetails1 = accountService.loadUserByUsername("root");
      assertNotNull(userDetails1);
      assertEquals(userDetails1.getUsername(), "Guest");
      TestFileUtils.deleteXMLFile(FileUtils.getConfigFile(UsersFile, "Users"));
      Users users3 = new Users();
      users3.setUsers(Arrays.asList(user2, user1));
      TestFileUtils.createXMLFile(users3, FileUtils.getConfigFile(UsersFile, "Users"));
      userDetails1 = accountService.loadUserByUsername("serengeti");
      assertNotNull(userDetails1);
      assertEquals(userDetails1.getUsername(), "serengeti");
      TestFileUtils.deleteXMLFile(FileUtils.getConfigFile(UsersFile, "Users"));
   }
}
