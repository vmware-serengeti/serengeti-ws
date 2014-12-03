package com.vmware.bdd.usermgmt; /******************************************************************************
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

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.TlsConnectionException;
import com.vmware.bdd.security.tls.TlsTcpClient;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 11/27/14.
 */
@Component
public class UserMgmtServerValidService {

   public void setTlsClient(TlsTcpClient tlsClient) {
      this.tlsClient = tlsClient;
   }

   @Autowired
   private TlsTcpClient tlsClient;

   @Autowired
   private SssdLdapConstantMappings sssdLdapConstantMappings;


   public void validateServerInfo(UserMgmtServer userMgmtServer, boolean forceTrustCert) {
      validateCertificate(userMgmtServer, forceTrustCert);

      searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());

   }

   public void validateCertificate(UserMgmtServer userMgmtServer, boolean forceTrustCert) {
      Pattern pattern = Pattern.compile("^(ldap(?:s?))\\:\\/\\/([-.\\w]*)(?:\\:([0-9]*))?(\\/.*)?$");

      Matcher matcher = pattern.matcher(userMgmtServer.getPrimaryUrl());

      if (matcher.matches()) {
         if ("ldaps".equals(matcher.group(1))) {
            String host = matcher.group(2);
            String port = matcher.group(3);
            int portNum = port == null ? 636 : Integer.parseInt(port);

            try {
               tlsClient.checkCertificateFirstly(host, portNum, forceTrustCert);
            } catch (TlsConnectionException tlse) {
               ValidationError error = new ValidationError("PrimaryUrl.CannotConnect", "Can not connect to the primary URL.");
               ValidationErrors errors = new ValidationErrors();
               errors.addError("PrimaryUrl", error);
               throw new ValidationException(errors.getErrors());
            }
         }
      }
   }

   protected void searchGroupDn(UserMgmtServer userMgmtServer, String groupDn) {
      // Set up the environment for creating the initial context
      Hashtable<String, Object> env = new Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

      // Specify LDAPS URL
      env.put(Context.PROVIDER_URL, userMgmtServer.getPrimaryUrl());

      // Authenticate as S. User and password "mysecret"
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, userMgmtServer.getUserName());
      env.put(Context.SECURITY_CREDENTIALS, userMgmtServer.getPassword());
      //  env.put(Context.SECURITY_PROTOCOL, "ssl");

      DirContext ctx = null;
      ValidationErrors validationErrors = new ValidationErrors();
      try {
         // Create the initial context
         ctx = new InitialDirContext(env);
         NamingEnumeration<SearchResult> answer = null;
         //validateServerInfo mgmt server default admin group
         try {
            answer = ctx.search(userMgmtServer.getBaseGroupDn(), null);

//            if (!answer.hasMoreElements()) {
//               validationErrors.addError("BaseGroupDn", new ValidationError("BASE_GROUP_DN.NOT_FOUND", "BaseGroupDn not found."));
//            } else {

            String groupCn = getCnFromGroupDn(groupDn);
            if (groupCn == null | groupCn.trim().length() == 0) {
               validationErrors.addError("AdminGroup", new ValidationError("ADMIN_GROUP.FORMAT_ERR", "CN attribute missing."));
            } else {
               answer = ctx.search(
                     userMgmtServer.getBaseGroupDn(),
                     "(&(objectClass={0}) (cn={1}))",
                     new Object[]{sssdLdapConstantMappings.get(userMgmtServer.getType(), SssdLdapConstantMappings.LDAP_GROUP_OBJECT_CLASS), groupCn}, null);
               if (!answer.hasMoreElements()) {
                  validationErrors.addError("AdminGroup", new ValidationError("ADMIN_GROUP.NOT_FOUND", "Admin Group not found."));
               }
            }
//            }
         } catch (NameNotFoundException nnf) {
            validationErrors.addError("BaseGroupDn", new ValidationError("BASE_GROUP_DN.NOT_FOUND", "BaseGroupDn not found."));
         } catch (InvalidNameException ine) {
            validationErrors.addError("BaseGroupDn", new ValidationError("BASE_GROUP_DN.INVALID_DN", "BaseGroupDn is not a valid DN."));
         }

         try {
            answer = ctx.search(userMgmtServer.getBaseUserDn(), null);
//            if (!answer.hasMoreElements()) {
//               validationErrors.addError("BaseUserDn", new ValidationError("BASE_USER_DN.NOT_FOUND", "BaseUserDn not found."));
//            }
         } catch (NameNotFoundException nnf) {
            validationErrors.addError("BaseUserDn", new ValidationError("BASE_USER_DN.NOT_FOUND", "BaseUserDn not found."));
         } catch (InvalidNameException ine) {
            validationErrors.addError("BaseUserDn", new ValidationError("BASE_USER_DN.INVALID_DN", "BaseUserDn is not a valid DN."));
         }
      } catch (AuthenticationException e) {
         validationErrors.addError("UserCredential", new ValidationError("UserCredential.Invalid", "invalid username or password."));
      } catch (InvalidNameException ine) {
         validationErrors.addError("UserName", new ValidationError("USERNAME.INVALID_DN", "UserName is not a valid DN."));
      } catch (CommunicationException ce) {
         validationErrors.addError("PrimaryUrl", new ValidationError("PrimaryUrl.CannotConnect", "Can not connect to the primary URL."));
      } catch (NamingException e) {
         throw new UserMgmtServerValidException(e);
      } finally {
         if (ctx != null) {
            try {
               ctx.close();
            } catch (NamingException e) {
               //
            }
         }

         if (!validationErrors.getErrors().isEmpty()) {
            throw new ValidationException(validationErrors.getErrors());
         }
      }
   }

   public String getCnFromGroupDn(String groupDn) {
      String cn = null;

      int index1 = groupDn.indexOf("cn=");

      if (index1 == -1) {
         index1 = groupDn.indexOf("CN=");
      }

      if (index1 != -1) {
         int index2 = groupDn.indexOf(',', index1);
         if (index2 != -1) {
            cn = groupDn.substring(index1 + 3, index2);
         }
      }

      return cn;
   }
}
