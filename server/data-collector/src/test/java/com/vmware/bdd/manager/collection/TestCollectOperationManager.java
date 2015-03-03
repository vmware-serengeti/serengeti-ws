/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.manager.collection;

import com.vmware.bdd.utils.PropertiesUtil;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.*;

public class TestCollectOperationManager {

    private MethodInvocationProceedingJoinPoint joinPoint;
    private CollectOperationManager collectOperationManager;
    private static File file;
    private static String FILE_PATH = "/tmp/test.properties";

    @MockClass(realClass = CollectionDriverManager.class)
    public static class MockCollectionDriverManager {
        @Mock
        public static File getConfigurationFile() {
            return file;
        }
    }

    @MockClass(realClass = PropertiesUtil.class)
    public static class MockPropertiesUtil {

        PropertiesUtil propertiesUtil = new PropertiesUtil(file);

        @Mock
        public String getProperty(String key) {
            return "true";
        }

        @Mock
        public PropertiesUtil setProperty(String key, String value) {
            Properties prop = new Properties();
            prop.setProperty(key, value);
            propertiesUtil.setProperties(prop);
            return propertiesUtil;
        }
    }

    private static void setUp() {
        file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @BeforeMethod(groups = { "TestCollectOperationManager" })
    public void setupMock() {
        setUp();
        Mockit.setUpMock(MockCollectionDriverManager.class);
        Mockit.setUpMock(MockPropertiesUtil.class);
        joinPoint = mock(MethodInvocationProceedingJoinPoint.class);
        Object[] args = new Object[] {false};
        Signature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(args);
        when(signature.getName()).thenReturn("getClusters");
        when(joinPoint.getSignature()).thenReturn(signature);
        collectOperationManager = new CollectOperationManager();
    }

    @AfterMethod
    public void tearDown() {
        Mockit.tearDownMocks();
        cleanUpData();
    }

    private void cleanUpData() {
        if (file.exists()) {
            file.delete();
        }
    }

    @Test(groups = { "TestCollectOperationManager" })
    public void testStoreOperationParameters() {
        CollectOperationManager.storeOperationParameters(joinPoint);
        List<Map<String, Object>> restData = collectOperationManager.consumeOperations();
        assertEquals(restData.size(), 1);
        assertEquals(restData.get(0).size(), 6);
        assertEquals(restData.get(0).get("operation_name"), "getClusters");
        assertEquals(restData.get(0).get("operation_status"), "COMPLETED");
        List params = (List)restData.get(0).get("operation_parameters");
        assertEquals(params.get(0), false);
    }

    @Test(groups = { "TestCollectOperationManager" })
    public void testStoreOperationParametersWithReturnValueParameter() {
        CollectOperationManager.storeOperationParameters(joinPoint, 1000L);
        List<Map<String, Object>> restData = collectOperationManager.consumeOperations();
        assertEquals(restData.size(), 1);
        assertEquals(restData.get(0).size(), 5);
        assertEquals(restData.get(0).get("operation_name"), "getClusters");
        assertEquals(restData.get(0).get("operation_status"), null);
        assertEquals(restData.get(0).get("end_time"), null);
        List params = (List)restData.get(0).get("operation_parameters");
        assertEquals(params.get(0), false);
    }
}
