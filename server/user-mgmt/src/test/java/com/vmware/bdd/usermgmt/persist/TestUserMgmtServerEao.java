/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.usermgmt.persist;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.dal.IBaseDAO;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.EncryptionGuard;
import com.vmware.bdd.usermgmt.TestUtils;
import mockit.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;


public class TestUserMgmtServerEao {
    private IBaseDAO<UserMgmtServerEntity> userMgmtServerDao;
    private final String name = "Name";
    private final String userName = "userName";
    private final static String password = "password";
    private final static String decodePassword = "decodePassword";
    private final String baseGroupDn = "baseGroupDn";
    private final String baseUserDn = "baseUserDn";
    private final String mgmtVMUserGroupDn = "mgmtVMUserGroupDn";
    private final String primaryUrl = "http://localhost1";
    private final String secondaryUrl = "http://localhost2";
    private final String NOT_FOUND_ERROR_MESSAGE = "given server is not found.";
    private final String NO_CHANGE_ERROR_MESSAGE = "The server info is not changed.";
    @Tested
    UserMgmtServerEao userMgmtServerEao;

    @MockClass(realClass = EncryptionGuard.class)
    public static class MockEncryptionGuard {
        @Mock
        public static String encode(String clearText) {
            return decodePassword;
        }

        @Mock
        public static String decode(String encodedText)
                throws GeneralSecurityException, UnsupportedEncodingException {
            return password;
        }
    }

    @BeforeMethod(groups = {"TestUserMgmtServerEao"})
    public void setupMock() {
        Mockit.setUpMock(MockEncryptionGuard.class);
    }

    @AfterMethod(groups = {"TestUserMgmtServerEao"})
    public void tearDown() {
        Mockit.tearDownMocks();
    }

    @Test(groups = {"TestUserMgmtServerEao"})
    public void testPersist() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                return null;
            }

            @Mock(invocations = 1)
            public void insert(UserMgmtServerEntity userMgmtServerEntity) {
                assertUserMgmtServerEntity(userMgmtServerEntity);
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = prepareUserMgmtServer(true);
        userMgmtServerEao.persist(userMgmtServer);
    }

    @Test(groups = {"TestUserMgmtServerEao"}, expectedExceptions = ValidationException.class)
    public void testPersistFailed() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = new UserMgmtServerEntity();
                userMgmtServerEntity.setName(name);
                return userMgmtServerEntity;
            }

            @Mock(invocations = 0)
            public void insert(UserMgmtServerEntity userMgmtServerEntity) {
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = new UserMgmtServer();
        userMgmtServerEao.persist(userMgmtServer);
    }

    @Test(groups = {"TestUserMgmtServerEao"})
    public void testFindByName() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 2)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = prepareUserMgmtServerEntity(decodePassword);
                return userMgmtServerEntity;
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);

        UserMgmtServer userMgmtServer = userMgmtServerEao.findByName(name, false);
        assertUserMgmtServer(userMgmtServer, false);

        userMgmtServer = userMgmtServerEao.findByName(name, true);
        assertUserMgmtServer(userMgmtServer, true);
    }

    @Test(groups = {"TestUserMgmtServerEao"})
    public void testDelete() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = new UserMgmtServerEntity();
                userMgmtServerEntity.setName(name);
                return userMgmtServerEntity;
            }

            @Mock(invocations = 1)
            public void delete(UserMgmtServerEntity userMgmtServerEntity) {
                assertEquals(userMgmtServerEntity.getName(), name);
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        userMgmtServerEao.delete(name);
    }

    @Test(groups = {"TestUserMgmtServerEao"}, expectedExceptions = ValidationException.class)
    public void testDeleteFailed() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                return null;
            }

            @Mock(invocations = 0)
            public void delete(UserMgmtServerEntity userMgmtServerEntity) {
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        userMgmtServerEao.delete(name);
    }

    @Test(groups = {"TestUserMgmtServerEao"})
    public void testCheckServerChanged() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = prepareUserMgmtServerEntity(decodePassword);
                return userMgmtServerEntity;
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = prepareUserMgmtServer(true);
        userMgmtServerEao.checkServerChanged(userMgmtServer);
    }

    @Test(groups = {"TestUserMgmtServerEao"}, expectedExceptions = ValidationException.class)
    public void testCheckServerChangedWithNotFoundException() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                return null;
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = prepareUserMgmtServer(true);
        try {
            userMgmtServerEao.checkServerChanged(userMgmtServer);
        } catch (ValidationException e) {
            assertEquals(e.getErrors().get("NAME").getMessage(), NOT_FOUND_ERROR_MESSAGE);
            throw e;
        }
    }

    @Test(groups = {"TestUserMgmtServerEao"}, expectedExceptions = ValidationException.class)
    public void testCheckServerChangedWithNoChangeException() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = prepareUserMgmtServerEntity(password);
                return userMgmtServerEntity;
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = prepareUserMgmtServer(false);
        try {
            userMgmtServerEao.checkServerChanged(userMgmtServer);
        } catch (ValidationException e) {
            assertEquals(e.getErrors().get("USERMGMTSERVER").getMessage(), NO_CHANGE_ERROR_MESSAGE);
            throw e;
        }
    }

    @Test(groups = {"TestUserMgmtServerEao"})
    public void testModify() {
        userMgmtServerDao = new MockUp<IBaseDAO<UserMgmtServerEntity>>() {
            @Mock(invocations = 1)
            public UserMgmtServerEntity findById(Serializable id) {
                UserMgmtServerEntity userMgmtServerEntity = prepareUserMgmtServerEntity(password);
                return userMgmtServerEntity;
            }

            @Mock(invocations = 1)
            public void update(UserMgmtServerEntity userMgmtServerEntity) {
                assertUserMgmtServerEntity(userMgmtServerEntity);
            }
        }.getMockInstance();
        TestUtils.setPrivateField(userMgmtServerEao, "userMgmtServerDao", userMgmtServerDao);
        UserMgmtServer userMgmtServer = prepareUserMgmtServer(true);
        userMgmtServerEao.modify(userMgmtServer);
    }

    private UserMgmtServer prepareUserMgmtServer(boolean usedSecondaryUrl) {
        UserMgmtServer userMgmtServer = new UserMgmtServer();
        userMgmtServer.setName(name);
        userMgmtServer.setPassword(password);
        userMgmtServer.setUserName(userName);
        userMgmtServer.setBaseGroupDn(baseGroupDn);
        userMgmtServer.setBaseUserDn(baseUserDn);
        userMgmtServer.setMgmtVMUserGroupDn(mgmtVMUserGroupDn);
        userMgmtServer.setPrimaryUrl(primaryUrl);
        if (usedSecondaryUrl) {
            userMgmtServer.setSecondaryUrl(secondaryUrl);
        }
        userMgmtServer.setType(UserMgmtServer.Type.LDAP);
        return userMgmtServer;
    }

    private UserMgmtServerEntity prepareUserMgmtServerEntity(final String password) {
        UserMgmtServerEntity userMgmtServerEntity = new UserMgmtServerEntity();
        userMgmtServerEntity.setName(name);
        userMgmtServerEntity.setPassword(password);
        userMgmtServerEntity.setUserName(userName);
        userMgmtServerEntity.setBaseGroupDn(baseGroupDn);
        userMgmtServerEntity.setBaseUserDn(baseUserDn);
        userMgmtServerEntity.setMgmtVMUserGroupDn(mgmtVMUserGroupDn);
        userMgmtServerEntity.setPrimaryUrl(primaryUrl);
        userMgmtServerEntity.setType(UserMgmtServer.Type.LDAP);
        return userMgmtServerEntity;
    }

    private void assertUserMgmtServerEntity(final UserMgmtServerEntity userMgmtServerEntity) {
        assertEquals(userMgmtServerEntity.getName(), name);
        assertEquals(userMgmtServerEntity.getPassword(), decodePassword);
        assertEquals(userMgmtServerEntity.getUserName(), userName);
        assertEquals(userMgmtServerEntity.getBaseGroupDn(), baseGroupDn);
        assertEquals(userMgmtServerEntity.getBaseUserDn(), baseUserDn);
        assertEquals(userMgmtServerEntity.getMgmtVMUserGroupDn(), mgmtVMUserGroupDn);
        assertEquals(userMgmtServerEntity.getPrimaryUrl(), primaryUrl);
        assertEquals(userMgmtServerEntity.getType(), UserMgmtServer.Type.LDAP);
    }

    private void assertUserMgmtServer(final UserMgmtServer userMgmtServer, boolean encrypted) {
        assertEquals(userMgmtServer.getName(), name);
        assertEquals(userMgmtServer.getPassword(), encrypted ? decodePassword : password);
        assertEquals(userMgmtServer.getUserName(), userName);
        assertEquals(userMgmtServer.getBaseGroupDn(), baseGroupDn);
        assertEquals(userMgmtServer.getBaseUserDn(), baseUserDn);
        assertEquals(userMgmtServer.getMgmtVMUserGroupDn(), mgmtVMUserGroupDn);
        assertEquals(userMgmtServer.getPrimaryUrl(), primaryUrl);
        assertEquals(userMgmtServer.getType(), UserMgmtServer.Type.LDAP);
    }
}
