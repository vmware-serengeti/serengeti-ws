/******************************************************************************
 *   Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.usermgmt;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.SimpleServerTrustManager;
import com.vmware.bdd.security.tls.SimpleSeverTrustTlsSocketFactory;
import com.vmware.bdd.security.tls.TlsConnectionException;
import com.vmware.bdd.security.tls.TlsTcpClient;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 11/27/14.
 */
@Component
public class UserMgmtServerValidService {
   private final static Logger LOGGER = Logger.getLogger(UserMgmtServerValidService.class);
   private static final String LDAP_GROUP_OBJECT_CLASS = "ldap_group_object_class";
   private static final String LDAP_USER_OBJECT_CLASS = "ldap_user_object_class";

   @Autowired
   private LdapsTrustStoreConfig ldapsTrustStoreConfig;

   @Autowired
   private SssdConfigurationGenerator sssdConfigurationGenerator;


   public void validateServerInfo(UserMgmtServer userMgmtServer, boolean forceTrustCert) {
      String[] ldapUrlElements = getLdapProtocol(userMgmtServer.getPrimaryUrl());

      boolean isLdaps = "LDAPS".equalsIgnoreCase(ldapUrlElements[0]);
      if (isLdaps) {
         validateCertificate(ldapUrlElements, forceTrustCert);
      }

      searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn(), isLdaps);

   }

   private String[] getLdapProtocol(String ldapUrl) {
      Pattern pattern = Pattern.compile("^(ldap(?:s?))\\:\\/\\/([-.\\w]*)(?:\\:([0-9]*))?(\\/.*)?$");

      Matcher matcher = pattern.matcher(ldapUrl);

      if (matcher.matches()) {
         return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
      }

      return null;
   }


   public void validateGroupUsers(UserMgmtServer userMgmtServer, Map<String, Set<String>> groupUsers) {
      searchGroupAndUser(userMgmtServer, groupUsers);
   }

   public void validateCertificate(String[] ldapUrlElements, boolean forceTrustCert) {
      if ("LDAPS".equalsIgnoreCase(ldapUrlElements[0])) {
         String host = ldapUrlElements[1];
         String port = ldapUrlElements[2];
         int portNum = port == null ? 636 : Integer.parseInt(port);

         TlsTcpClient tlsTcpClient = new TlsTcpClient();
         SimpleServerTrustManager simpleServerTrustManager = new SimpleServerTrustManager();
         simpleServerTrustManager.setTrustStoreConfig(ldapsTrustStoreConfig);
         tlsTcpClient.setTrustManager(simpleServerTrustManager);
         try {
            tlsTcpClient.checkCertificateFirstly(host, portNum, forceTrustCert);
         } catch (TlsConnectionException tlse) {
            ValidationError error = new ValidationError("PrimaryUrl.CannotConnect", "Can not connect to the primary URL.");
            ValidationErrors errors = new ValidationErrors();
            errors.addError("PrimaryUrl", error);
            throw new ValidationException(errors.getErrors());
         }
      }
   }

   public void searchGroup(UserMgmtServer userMgmtServer, String[] groupNames) {
      Map<String, Set<String>> groupUsers = new HashMap<>();
      for (String groupName: groupNames) {
         groupUsers.put(groupName, null);
      }
      searchGroupAndUser(userMgmtServer, groupUsers);
   }

   //groupUsers is a groupName to group users map. For group without user, the String[] can be null or empty
   public void searchGroupAndUser(UserMgmtServer userMgmtServer, Map<String, Set<String>> groupUsers) {
      String[] groupNames = new String[groupUsers.keySet().size()];
      groupUsers.keySet().toArray(groupNames);
      String[] ldapUrlElements = getLdapProtocol(userMgmtServer.getPrimaryUrl());

      boolean isLdaps = "LDAPS".equalsIgnoreCase(ldapUrlElements[0]);

      Hashtable<String, Object> env = new Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

      // Specify LDAPS URL
      env.put(Context.PROVIDER_URL, userMgmtServer.getPrimaryUrl());

      // Authenticate as Simple username and password
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, userMgmtServer.getUserName());
      env.put(Context.SECURITY_CREDENTIALS, userMgmtServer.getPassword());

      if (isLdaps) {
         //@TODO it's interesting to add a custom socket factory to set SO_TIMEOUT
         SimpleSeverTrustTlsSocketFactory.init(ldapsTrustStoreConfig);
         env.put("java.naming.ldap.factory.socket", "com.vmware.bdd.security.tls.SimpleSeverTrustTlsSocketFactory");
      }

      // env.put(Context.SECURITY_PROTOCOL, "ssl");

      DirContext ctx = null;
      ValidationErrors validationErrors = new ValidationErrors();
      String groupObjectClass = sssdConfigurationGenerator.get(userMgmtServer.getType()).get(LDAP_GROUP_OBJECT_CLASS);
      String userObjectClass = sssdConfigurationGenerator.get(userMgmtServer.getType()).get(LDAP_USER_OBJECT_CLASS);
      try {
         // Create the initial context
         ctx = new InitialDirContext(env);
         NamingEnumeration<SearchResult> answer = null;
         //validateServerInfo mgmt server default admin group
         for(String groupName : groupNames) {
            try {
               answer = ctx.search(
                     userMgmtServer.getBaseGroupDn(),
                     "(&(objectClass={0}) (cn={1}))",
                     new Object[]{groupObjectClass, groupName}, null);
               if (!answer.hasMoreElements()) {
                  validationErrors.addError(groupName, new ValidationError("GROUP.NOT_FOUND", String.format("Group (%1s) not found.", groupName)));
               }
               Set<String> users = groupUsers.get(groupName);
               if (users != null && !users.isEmpty()) {
                  for (String user: users) {
                     String memberOf = "cn=" + groupName + "," + userMgmtServer.getBaseGroupDn();
                     answer = ctx.search(
                           userMgmtServer.getBaseUserDn(),
                           "(&(objectClass={0}) (cn={1}) (memberOf={2}))",
                           new Object[]{userObjectClass, user, memberOf}, null);
                     if (!answer.hasMoreElements()) {
                        validationErrors.addError(user, new ValidationError("USER.NOT_FOUND", String.format("User (%1s) not found in group (%2s).", user, groupName)));
                     }
                  }
               }
            } catch (NameNotFoundException nnf) {
               validationErrors.addError("BaseGroupDn", new ValidationError("BASE_GROUP_DN.NOT_FOUND", "BaseGroupDn not found."));
            } catch (InvalidNameException ine) {
               validationErrors.addError("BaseGroupDn", new ValidationError("BASE_GROUP_DN.INVALID_DN", "BaseGroupDn is not a valid DN."));
            }
         }
      } catch (AuthenticationException e) {
         validationErrors.addError("UserCredential", new ValidationError("UserCredential.Invalid", "invalid username or password."));
      } catch (InvalidNameException ine) {
         validationErrors.addError("UserName", new ValidationError("USERNAME.INVALID_DN", "UserName is not a valid DN."));
      } catch (CommunicationException ce) {
         LOGGER.warn(ce.getMessage());
         LOGGER.warn(ce.getCause().getMessage());
         ce.printStackTrace();
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
            LOGGER.error(validationErrors.getErrors());
            throw new ValidationException(validationErrors.getErrors());
         }
      }
   }

   protected void searchGroupDn(UserMgmtServer userMgmtServer, String groupDn, boolean isLdaps) {
      Hashtable<String, Object> env = new Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

      // Specify LDAPS URL
      env.put(Context.PROVIDER_URL, userMgmtServer.getPrimaryUrl());

      // Authenticate as Simple username and password
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, userMgmtServer.getUserName());
      env.put(Context.SECURITY_CREDENTIALS, userMgmtServer.getPassword());

      if (isLdaps) {
         //@TODO it's interesting to add a custom socket factory to set SO_TIMEOUT
         SimpleSeverTrustTlsSocketFactory.init(ldapsTrustStoreConfig);
         env.put("java.naming.ldap.factory.socket", "com.vmware.bdd.security.tls.SimpleSeverTrustTlsSocketFactory");
      }

      //  env.put(Context.SECURITY_PROTOCOL, "ssl");
      String groupObjectClass = sssdConfigurationGenerator.get(userMgmtServer.getType()).get(LDAP_GROUP_OBJECT_CLASS);

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
                     new Object[]{groupObjectClass, groupCn}, null);
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
         LOGGER.warn(ce.getMessage());
         LOGGER.warn(ce.getCause().getMessage());
         ce.printStackTrace();
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
