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
package com.vmware.bdd.web.servlet.filter;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class TestMaintenanceModeAccessFilter {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private PrintWriter printWriter;
    @Mock
    private File file;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterMethod
    public void clean() {

    }

    @Test
    public void testDoFilter() throws IOException, ServletException {
        MaintenanceModeAccessFilter maintenanceModeAccessFilter = Mockito.spy(new MaintenanceModeAccessFilter());
        when(response.getWriter()).thenReturn(printWriter);
        when(file.exists()).thenReturn(true);
        Mockito.when(maintenanceModeAccessFilter.getMaintenanceModeFlagFile()).thenReturn(file);
        Mockito.when(request.getMethod()).thenReturn("GET");
        maintenanceModeAccessFilter.doFilter(request, response, chain);

        verify(request, times(1)).getMethod();
        verify(response, times(0)).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response, times(0)).getWriter();
        verify(chain, times(1)).doFilter(request, response);

        Mockito.when(request.getMethod()).thenReturn("HEAD");
        maintenanceModeAccessFilter.doFilter(request, response, chain);
        verify(request, times(2)).getMethod();
        verify(response, times(0)).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response, times(0)).getWriter();
        verify(chain, times(2)).doFilter(request, response);

        Mockito.when(request.getMethod()).thenReturn("POST");
        maintenanceModeAccessFilter.doFilter(request, response, chain);
        verify(request, times(3)).getMethod();
        verify(response, times(1)).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response, times(2)).getWriter();
        verify(chain, times(2)).doFilter(request, response);

        when(file.exists()).thenReturn(false);
        maintenanceModeAccessFilter.doFilter(request, response, chain);
        verify(request, times(3)).getMethod();
        verify(response, times(1)).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response, times(2)).getWriter();
        verify(chain, times(3)).doFilter(request, response);
    }
}
