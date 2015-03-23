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
package com.vmware.bdd.aop.collection;

import com.vmware.bdd.manager.collection.*;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.collection.impl.FakeCollectionInitializerService;
import com.vmware.bdd.utils.PropertiesUtil;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.aspectj.lang.JoinPoint;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import org.aspectj.lang.Signature;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJobListenerAdvice {

    private CollectionDriverManager collectionDriverManager;
    private static CollectionDriver collectionDriver;
    private ICollectionInitializerService collectionInitializerService;
    private CollectOperationManager collectOperationManager;
    private DataContainer dataContainer;
    private JobListenerAdvice jobListenerAdvice;
    private JoinPoint joinPoint;
    private JobExecution jobExecution;
    private ExitStatus exitStatus;
    private static File file;
    private static String FILE_PATH = "/tmp/test.properties";

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

    @BeforeMethod(groups = { "TestJobListenerAdvice" })
    public void setupMock() {
        setUp();
        Mockit.setUpMock(MockPropertiesUtil.class);
        collectionDriver = mock(FakeCollectionDriver.class);
        collectionInitializerService = mock(FakeCollectionInitializerService.class);
        collectOperationManager = mock(CollectOperationManager.class);
        dataContainer = new DataContainer();
        collectionDriverManager = new FakeCollectionDriverManager();
        collectionDriverManager.setDriverClass("com.vmware.bdd.manager.ph.PhoneHomeCollectionDriver");
        collectionDriverManager.setCollectionInitializerService(collectionInitializerService);
        collectionDriverManager.setDriver(collectionDriver);
        collectionDriverManager.setCollectOperationManager(collectOperationManager);
        collectionDriverManager.setDataContainer(dataContainer);
        collectionDriverManager.setFile(file);
        when(collectionDriver.getCollectionSwitchName()).thenReturn("serengeti.ph.enable");
        jobListenerAdvice = new JobListenerAdvice();
        jobListenerAdvice.setCollectionDriverManager(collectionDriverManager);
        jobListenerAdvice.setDataContainer(dataContainer);

        joinPoint = mock(JoinPoint.class);
        jobExecution = mock(JobExecution.class);
        when(jobExecution.getId()).thenReturn(1000L);
        exitStatus = mock(ExitStatus.class);
        when(exitStatus.getExitCode()).thenReturn(ExitStatus.COMPLETED.getExitCode());
        when(jobExecution.getExitStatus()).thenReturn(exitStatus);
        Object[] args = new Object[] {jobExecution};
        when(joinPoint.getArgs()).thenReturn(args);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("signatureName");
        when(joinPoint.getSignature()).thenReturn(signature);
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

    @Test(groups = { "TestJobListenerAdvice" })
    public void testJobAfter() {
        dataContainer.setMaxLength(1);
        jobListenerAdvice.jobAfter(joinPoint);
        Map<String, Map<String, Object>> data = dataContainer.pop();
        assertEquals(data.get("asynchronization_1000").size(), 2);
        assertNotNull(data.get("asynchronization_1000").get("end_time"));
        assertEquals(data.get("asynchronization_1000").get("operation_status"), "COMPLETED");
    }
}
