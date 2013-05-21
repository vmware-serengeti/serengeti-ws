package com.vmware.bdd.security.sso;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.opensaml.saml2.core.Assertion;
import org.testng.annotations.Test;

import com.vmware.bdd.utils.FileUtils;

public class TestVCSamlTokenAuthenticationFilter extends EasyMockSupport {
   private static final String SPRING_SECURITY_FROM_SAML_TOKEN_KEY = "VCSSOToken";
   private static final String SSO_XML_FILE = "ssotoken.xml"; 

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
      VCSamlTokenAuthenticationFilter samlAuthenticationFilter = new VCSamlTokenAuthenticationFilter();
      Assertion assertion = samlAuthenticationFilter.obtainSamlToken(request);
      assertEquals(assertion.getSubject().getNameID()
            .getValue(), "lzhai@aurora.dev");
      control.verify();
   }

}