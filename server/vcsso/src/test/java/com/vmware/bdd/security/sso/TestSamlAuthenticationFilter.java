/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.security.sso;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.opensaml.saml2.core.Response;
import org.testng.annotations.Test;

import com.vmware.bdd.security.sso.SamlAuthenticationFilter;
import com.vmware.bdd.utils.FileUtils;

public class TestSamlAuthenticationFilter extends EasyMockSupport {

   private static final String SPRING_SECURITY_FROM_SAML_TOKEN_KEY = "SAMLResponse";
   private static final String SSO_XML_FILE = "sso.xml"; 

   @Test
   public void testObtainSamlToken() throws Exception {
      IMocksControl control = EasyMock.createControl();
      HttpServletRequest request = control.createMock(HttpServletRequest.class);
      File ssoFile = FileUtils.getConfigFile(SSO_XML_FILE, "SSO");
      String samlToken = FileUtils.obtainStringFromFile(ssoFile);
      String encodeSamlToken = org.opensaml.xml.util.Base64.encodeBytes(samlToken.getBytes());
      EasyMock
            .expect(request.getParameter(SPRING_SECURITY_FROM_SAML_TOKEN_KEY))
            .andReturn(encodeSamlToken);
      control.replay();
      SamlAuthenticationFilter samlAuthenticationFilter = new SamlAuthenticationFilter();
      Response response = samlAuthenticationFilter.obtainSamlToken(request);
      assertEquals(response.getAssertions().get(0).getSubject().getNameID()
            .getValue(), "lzhai@aurora.dev");
      control.verify();
   }

}
