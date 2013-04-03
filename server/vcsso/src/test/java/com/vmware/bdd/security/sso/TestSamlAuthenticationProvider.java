package com.vmware.bdd.security.sso;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.X509Data;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.testng.annotations.Test;

import com.vmware.bdd.security.sso.SamlAuthenticationFilter;
import com.vmware.bdd.security.sso.SamlAuthenticationProvider;
import com.vmware.bdd.security.sso.utils.SecurityUtils;
import com.vmware.bdd.utils.FileUtils;
import com.vmware.vim.sso.client.DefaultSecurityTokenServiceFactory;
import com.vmware.vim.sso.client.DefaultTokenFactory;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.SecurityTokenService;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig.ConnectionConfig;


public class TestSamlAuthenticationProvider  extends EasyMockSupport {

   private static final String SPRING_SECURITY_FROM_SAML_TOKEN_KEY = "SAMLResponse";
   private static final String SSO_XML_FILE = "sso.xml";

   //@Test
   public void testAuthenticate() throws Exception {
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
      control.reset();
      Authentication authentication = control.createMock(Authentication.class);
      EasyMock
            .expect(authentication.getCredentials())
            .andReturn(response);
      control.replay();
      SamlAuthenticationProvider provider = new SamlAuthenticationProvider();
      provider.authenticate(authentication);
      control.verify();
   }
   
   //@Test
   public void testSamlTokenVerifyBySSO() throws Exception {
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
      
      //test verify saml token by sso
      URL stsURL = new URL("https://10.110.170.6:7444/ims/STSService?wsdl");
      FileInputStream bis = new FileInputStream("c:\\bdc\\sdk\\sts.crt");
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      List<X509Certificate> stsCerts = new ArrayList<X509Certificate>();
      while (bis.available() > 0) {
         stsCerts.add((X509Certificate)cf.generateCertificate(bis));
      }
      X509Certificate[] certs = stsCerts.toArray(new X509Certificate[stsCerts.size()]);
      ConnectionConfig connConfig = new ConnectionConfig(stsURL, certs, null);
      SecurityTokenServiceConfig config = new SecurityTokenServiceConfig(connConfig, connConfig.getTrustedRootCertificates(), null);

      // Create STS client
      SecurityTokenService stsClient = DefaultSecurityTokenServiceFactory.getSecurityTokenService(config);
      Assertion assertion = response.getAssertions().get(0);
      X509Certificate[] xcerts = getCertsFromAssertion(response.getAssertions().get(0));
      if (certs != null) {
         SamlToken ssoSamlToken =
               DefaultTokenFactory.createTokenFromDom(assertion.getDOM(),
                     xcerts);
         boolean validFromSSO = stsClient.validateToken(ssoSamlToken);
         if (!validFromSSO) {
            throw new BadCredentialsException("invalid saml token.");
         }
      }
      
   }

   private X509Certificate[] getCertsFromAssertion(Assertion assertion)
         throws CertificateException {
      List<X509Certificate> certList = new ArrayList<X509Certificate>();
      Signature ds = assertion.getSignature();
      if (ds != null) {
         List<X509Data> x509Data = ds.getKeyInfo().getX509Datas();
         certList = SecurityUtils.getCertsFromx509Data(x509Data);
         if (certList.size() > 0) {
            X509Certificate[] certs =
                  certList.toArray(new X509Certificate[certList.size()]);
            return certs;
         }
      }
      return null;
   }
}
