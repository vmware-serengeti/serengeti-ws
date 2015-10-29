/******************************************************************************
 *   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.web;


import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;

import mockit.MockClass;
import mockit.Mockit;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.testng.Assert.assertTrue;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.Date;

import static org.mockito.Mockito.*;

public class TestResourceInitializer {

    private static boolean booleanFlag = true;
    private static boolean endSessionFlag = false;
    @Mock
    private static WebApplicationContext webApplicationContext;
    @Mock
    private static IClusteringService clusteringService;
    @Mock
    private static ICollectionInitializerService collectionInitializerService;
    @Mock
    private static IResourceInitializerService resourceInitializerService;
    @MockClass(realClass = WebApplicationContextUtils.class)
    public static class MockWebApplicationContextUtils {
        @mockit.Mock
        public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
            when(webApplicationContext.getBean("clusteringService", IClusteringService.class))
                    .thenReturn(clusteringService);
            when(webApplicationContext.getBean("collectionInitializerService", ICollectionInitializerService.class))
                    .thenReturn(collectionInitializerService);
            when(webApplicationContext.getBean(IResourceInitializerService.class))
                    .thenReturn(resourceInitializerService);
            return webApplicationContext;
        }
    }

    @MockClass(realClass = Configuration.class)
    public static class MockConfiguration {
        @mockit.Mock
        public static Boolean getBoolean(String key) {
            return booleanFlag;
        }
    }

    @MockClass(realClass = VcContext.class)
    public static class MockVcContext {
        @mockit.Mock
        public static void endSession() {
            endSessionFlag = true;
        }
    }
    @Mock
    private ServletContextEvent sce;

    @InjectMocks
    private ResourceInitializer resourceInitializer;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockit.setUpMock(MockWebApplicationContextUtils.class);
        Mockit.setUpMock(MockConfiguration.class);
        Mockit.setUpMock(MockVcContext.class);
    }

    @AfterMethod
    public void resetFlag() {
        booleanFlag = true;
        endSessionFlag = false;
    }

    @Test
    public void testContextInitialized() {
        resourceInitializer.contextInitialized(sce);
        verify(sce, times(1)).getServletContext();
        verify(clusteringService, times(1)).init();
        verify(collectionInitializerService, times(1)).setDeployTime(Mockito.any(Date.class));
        verify(collectionInitializerService, times(1)).generateInstanceId();
        verify(resourceInitializerService, times(1)).isResourceInitialized();
        verify(resourceInitializerService, times(1)).initResource();
        verify(resourceInitializerService, times(1)).updateOrInsertServerInfo();

        when(resourceInitializerService.isResourceInitialized()).thenReturn(true);
        resourceInitializer.contextInitialized(sce);
        verify(sce, times(2)).getServletContext();
        verify(clusteringService, times(2)).init();
        verify(collectionInitializerService, times(2)).setDeployTime(Mockito.any(Date.class));
        verify(collectionInitializerService, times(2)).generateInstanceId();
        verify(resourceInitializerService, times(2)).isResourceInitialized();
        verify(resourceInitializerService, times(1)).initResource();
        verify(resourceInitializerService, times(1)).updateOrInsertServerInfo();

        booleanFlag = false;
        when(resourceInitializerService.isResourceInitialized()).thenReturn(false);
        resourceInitializer.contextInitialized(sce);
        verify(sce, times(3)).getServletContext();
        verify(clusteringService, times(3)).init();
        verify(collectionInitializerService, times(3)).setDeployTime(Mockito.any(Date.class));
        verify(collectionInitializerService, times(3)).generateInstanceId();
        verify(resourceInitializerService, times(3)).isResourceInitialized();
        verify(resourceInitializerService, times(1)).initResource();
        verify(resourceInitializerService, times(2)).updateOrInsertServerInfo();
    }

    @Test
    public void testContextDestroyed() {
        resourceInitializer.contextDestroyed(sce);
        verify(clusteringService).destroy();
        assertTrue(endSessionFlag);
    }
}
