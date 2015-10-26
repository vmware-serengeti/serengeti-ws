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
package com.vmware.bdd.rest.interceptor;

import com.vmware.bdd.utils.CommonUtil;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class TestRestLogInterceptor {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;
    private Exception ex;

    @MockClass(realClass = CommonUtil.class)
    public static class MockCommonUtil {
        @Mock
        public static void writeFile(File file, String content, boolean append) {
            if (System.getProperty("serengeti.home.dir") == null) {
                assertEquals(file.getAbsolutePath(), "/tmp/serengeti/logs/serengeti.audit");
            } else {
                assertEquals(file.getAbsolutePath(), "/opt/serengeti/logs/serengeti.audit");
            }
            assertTrue(content.contains("localhost, GET /api/hello, 200"));
            assertTrue(append);
        }
    }

    @BeforeMethod(groups = { "TestRestLogInterceptor" })
    public void setupMock() {
        Mockit.setUpMock(MockCommonUtil.class);
        request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("");
        when(request.getRemoteHost()).thenReturn("localhost");
        when(request.getRequestURI()).thenReturn("/api/hello");
        when(request.getMethod()).thenReturn("GET");
        response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        handler = mock(Object.class);
        ex = mock(Exception.class);
    }

    @Test(groups = { "TestRestLogInterceptor" })
    public void testAfterCompletion() {
        RestLogInterceptor testLogInterceptor = new RestLogInterceptor();
        try {
            testLogInterceptor.afterCompletion(request, response, handler, ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
