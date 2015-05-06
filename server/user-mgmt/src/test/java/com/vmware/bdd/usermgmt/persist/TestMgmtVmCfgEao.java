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

import com.vmware.bdd.dal.IBaseDAO;
import com.vmware.bdd.usermgmt.TestUtils;
import mockit.Mock;
import mockit.MockUp;
import mockit.Tested;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class TestMgmtVmCfgEao {
    private IBaseDAO<MgmtVmCfgItemEntity> mgmtVmCfgDao;
    private String ngmtVmCfgItemName = "ngmtVmCfgItemName";
    private String ngmtVmCfgItemValue = "ngmtVmCfgItemValue";
    private String ngmtVmCfgItemNewValue = "ngmtVmCfgItemNewValue";
    @Tested
    MgmtVmCfgEao mgmtVmCfgEao;

    @BeforeClass
    public void beforeClass() {
        mgmtVmCfgEao = new MgmtVmCfgEao();
    }

    @Test(groups = { "TestMgmtVmCfgEao" })
    public void testFindAll() {
        mgmtVmCfgDao = new MockUp<IBaseDAO<MgmtVmCfgItemEntity>>() {
            @Mock
            List<MgmtVmCfgItemEntity> findAll() {
                List<MgmtVmCfgItemEntity> mgmtVmCfgItemEntitys =
                        new ArrayList<MgmtVmCfgItemEntity>();
                MgmtVmCfgItemEntity mgmtVmCfgItemEntity = new MgmtVmCfgItemEntity();
                mgmtVmCfgItemEntity.setName(ngmtVmCfgItemName);
                mgmtVmCfgItemEntity.setValue(ngmtVmCfgItemValue);
                mgmtVmCfgItemEntitys.add(mgmtVmCfgItemEntity);
                return mgmtVmCfgItemEntitys;
            }
        }.getMockInstance();
        TestUtils.setPrivateField(mgmtVmCfgEao, "mgmtVmCfgDao", mgmtVmCfgDao);
        Map<String, String> mgmtVmCfgItemEntityMap=  mgmtVmCfgEao.findAll();
        assertEquals(mgmtVmCfgItemEntityMap.size(), 1);
        assertTrue(mgmtVmCfgItemEntityMap.containsKey(ngmtVmCfgItemName));
        assertEquals(mgmtVmCfgItemEntityMap.get(ngmtVmCfgItemName), ngmtVmCfgItemValue);
    }

    @Test(groups = { "TestMgmtVmCfgEao" })
    public void testUpdate() {
        mgmtVmCfgDao = new MockUp<IBaseDAO<MgmtVmCfgItemEntity>>() {
            @Mock
            public MgmtVmCfgItemEntity findById(Serializable id) {
                MgmtVmCfgItemEntity mgmtVmCfgItemEntity = new MgmtVmCfgItemEntity();
                mgmtVmCfgItemEntity.setName(ngmtVmCfgItemName);
                mgmtVmCfgItemEntity.setValue(ngmtVmCfgItemValue);
                return mgmtVmCfgItemEntity;
            }

            @Mock(invocations = 1)
            public void update(MgmtVmCfgItemEntity mgmtVmCfgItemEntity) {
                assertEquals(mgmtVmCfgItemEntity.getValue(), ngmtVmCfgItemNewValue);
            }

            @Mock(invocations = 0)
            public void insert(MgmtVmCfgItemEntity mgmtVmCfgItemEntity) {}

        }.getMockInstance();
        TestUtils.setPrivateField(mgmtVmCfgEao, "mgmtVmCfgDao", mgmtVmCfgDao);
        Map<String, String> map = new HashMap<>();
        map.put(ngmtVmCfgItemName, ngmtVmCfgItemNewValue);
        mgmtVmCfgEao.update(map);
    }

    @Test(groups = { "TestMgmtVmCfgEao" })
    public void testInsert() {
        mgmtVmCfgDao = new MockUp<IBaseDAO<MgmtVmCfgItemEntity>>() {
            @Mock
            public MgmtVmCfgItemEntity findById(Serializable id) {
                return null;
            }

            @Mock(invocations = 0)
            public void update(MgmtVmCfgItemEntity mgmtVmCfgItemEntity) {}

            @Mock(invocations = 1)
            public void insert(MgmtVmCfgItemEntity mgmtVmCfgItemEntity) {
                assertEquals(mgmtVmCfgItemEntity.getName(), ngmtVmCfgItemName);
                assertEquals(mgmtVmCfgItemEntity.getValue(), ngmtVmCfgItemValue);
            }
        }.getMockInstance();
        TestUtils.setPrivateField(mgmtVmCfgEao, "mgmtVmCfgDao", mgmtVmCfgDao);
        Map<String, String> map = new HashMap<>();
        map.put(ngmtVmCfgItemName, ngmtVmCfgItemValue);
        mgmtVmCfgEao.update(map);
    }

}
