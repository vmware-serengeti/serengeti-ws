/******************************************************************************
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
package com.vmware.bdd.usermgmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;

/**
 * Created By xiaoliangl on 12/31/14.
 */
@Component
public class SssdConfigurationGenerator {

   private final static String TEMPLATE_RESOURCE = "/com/vmware/bdd/usermgmt/sssd.conf.template";

   @Autowired
   private SssdLdapConstantMappings sssdLdapConstantMappings;
   
   private volatile StringBuilder[] templateContent = new StringBuilder[]{null};

   public SssdConfigurationGenerator() {
      if(SssdConfigurationGenerator.class.getResource(TEMPLATE_RESOURCE) == null) {
         throw new UserMgmtException("SSSD_CONF_TEMPLATE_NOT_FOUND", null);
      }
   }

   public void setSssdLdapConstantMappings(SssdLdapConstantMappings sssdLdapConstantMappings) {
      this.sssdLdapConstantMappings = sssdLdapConstantMappings;
   }

   protected void setTemplateContent(StringBuilder content) {
      templateContent[0] = content;
   }

   protected StringBuilder getTemplateContent() {
      return templateContent[0];
   }

   protected boolean isTemplateContentEmpty() {
      return templateContent[0] == null;
   }

   protected void load() {
      if(isTemplateContentEmpty()) {

         synchronized (templateContent) {
            InputStream templateResStream = SssdConfigurationGenerator.class.getResourceAsStream(TEMPLATE_RESOURCE);

            if (templateResStream == null) {
               throw new UserMgmtException("SSSD_CONF_TEMPLATE_NOT_FOUND", null);
            }

            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader templateBufReader = new BufferedReader(new InputStreamReader(templateResStream))) {
               String line = templateBufReader.readLine();
               while (line != null) {
                  stringBuilder.append(line).append('\n');
                  line = templateBufReader.readLine();
               }
            } catch (IOException ioe) {
               throw new UserMgmtException("SSSD_CONF_TEMPLATE_READ_ERR", ioe);
            }

            setTemplateContent(stringBuilder);
         }
      }
   }

   public String getConfigurationContent(UserMgmtServer userMgmtServer, String[] groups) {
      load();

      String configContent = new String(getTemplateContent());
      SssdLdapConstantMappings.Getter constantGetter = sssdLdapConstantMappings.getterFirst(userMgmtServer.getType());

      ArrayList<String[]> replacementList = new ArrayList<>();
      
      replacementList.add(new String[]{"LDAP_SCHEMA_VALUE", constantGetter.get(SssdLdapConstantMappings.LDAP_SCHEMA)});
      replacementList.add(new String[]{"LDAP_USER_OBJECT_CLASS_VALUE", constantGetter.get(SssdLdapConstantMappings.LDAP_USER_OBJECT_CLASS)});

      replacementList.add(new String[]{"LDAP_USER_NAME_VALUE", constantGetter.get(SssdLdapConstantMappings.LDAP_USER_NAME)});
      replacementList.add(new String[]{"LDAP_GROUP_OBJECT_CLASS_VALUE", constantGetter.get(SssdLdapConstantMappings.LDAP_GROUP_OBJECT_CLASS)});

      replacementList.add(new String[]{"LDAP_TLS_REQCERT_VALUE", "never"});

      replacementList.add(new String[]{"LDAP_GROUP_SEARCH_BASE_VALUE", userMgmtServer.getBaseGroupDn()});
      replacementList.add(new String[]{"LDAP_USER_SEARCH_BASE_VALUE", userMgmtServer.getBaseUserDn()});
      replacementList.add(new String[]{"LDAP_URI_VALUE", userMgmtServer.getPrimaryUrl()});
      replacementList.add(new String[]{"LDAP_DEFAULT_BIND_DN_VALUE", userMgmtServer.getUserName()});
      replacementList.add(new String[]{"LDAP_DEFAULT_AUTHTOK_VALUE", userMgmtServer.getPassword()});

      //enable auth by groupdn
      replacementList.add(new String[]{"ACCESS_PROVIDER_VALUE", "ldap"});

      StringBuilder stringBuilder = new StringBuilder();

      if(groups.length > 1) {
         stringBuilder.append("(|");
         for(String group : groups) {
            stringBuilder.append("(memberOf=cn=").append(group).append(",").append(userMgmtServer.getBaseGroupDn()).append(')');
         }
         stringBuilder.append(')');
      } else {
         stringBuilder.append("memberOf=cn=").append(groups[0]).append(",").append(userMgmtServer.getBaseGroupDn());
      }


      replacementList.add(new String[]{"LDAP_ACCESS_FILTER_VALUE", stringBuilder.toString()});

      for(String[] replacement : replacementList) {
         configContent = StringUtils.replace(configContent, replacement[0], replacement[1]);
      }
      
      return configContent;
   }

}
