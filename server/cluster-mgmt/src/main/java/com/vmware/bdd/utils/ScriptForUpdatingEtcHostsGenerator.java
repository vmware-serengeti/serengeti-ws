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
package com.vmware.bdd.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;

public class ScriptForUpdatingEtcHostsGenerator {

   private final static String TEMPLATE_RESOURCE = "/com/vmware/bdd/hostname_generating/script-for-updating-etc-hosts.template";
   private static final Logger logger = Logger.getLogger(ScriptForUpdatingEtcHostsGenerator.class);

   private volatile StringBuilder[] templateContent = new StringBuilder[]{null};
   private FileWriter ScriptForUpdatingEtcHosts;

   public ScriptForUpdatingEtcHostsGenerator() {
      if(ScriptForUpdatingEtcHostsGenerator.class.getResource(TEMPLATE_RESOURCE) == null) {
         throw BddException.SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_NOT_FOUND();
      }
   }

   public String generateScriptForUpdatingEtcHosts(String clusterName, String hostsContent) {
      String scriptName = clusterName;
      String scriptPath = Constants.SRCIPT_FOR_UPDATING_ETC_HOSTS_DIR + scriptName;

      File scriptForUpdatingEtcHostsDir = new File(Constants.SRCIPT_FOR_UPDATING_ETC_HOSTS_DIR);
      if (!scriptForUpdatingEtcHostsDir.exists()) {
         try {
            scriptForUpdatingEtcHostsDir.mkdir();
         } catch (BddException e) {
            logger.error("Faid to create script for updating /etc/hosts directory " + Constants.SRCIPT_FOR_UPDATING_ETC_HOSTS_DIR + ".");
            throw BddException.FAILED_TO_GENERATE_SCRIPT_FOR_UPDATING_ETC_HOSTS(e, e.getMessage());
         }
      }

      try {
         File oldScriptForUpdatingEtcHosts = new File(scriptPath);
         if (oldScriptForUpdatingEtcHosts.exists()) {
            oldScriptForUpdatingEtcHosts.delete();
         }

         ScriptForUpdatingEtcHosts = new FileWriter(scriptPath, true);
         ScriptForUpdatingEtcHosts.write(getContentForUpdatingEtcHosts(hostsContent));

         logger.info("Generate script for updating /etc/hosts " + scriptPath + " successfully.");
      } catch (Exception e) {
         logger.error("Faid to generate script for updating /etc/hosts " + scriptPath + ".");
         throw BddException.FAILED_TO_GENERATE_SCRIPT_FOR_UPDATING_ETC_HOSTS(e, e.getMessage());
      } finally {
         if (ScriptForUpdatingEtcHosts != null) {
            try {
               ScriptForUpdatingEtcHosts.close();
            } catch (IOException e) {
               throw BddException.FAILED_TO_GENERATE_SCRIPT_FOR_UPDATING_ETC_HOSTS(e, e.getMessage());
            }
         }
      }
      return scriptName;
   }

   public String getContentForUpdatingEtcHosts(String hostsContent) {
      load();

      String scriptContent = new String(getTemplateContent());

      ArrayList<String[]> replacementList = new ArrayList<>();

      replacementList.add(new String[]{"ETC_HOSTS_CONTENT", hostsContent});

      for(String[] replacement : replacementList) {
         scriptContent = StringUtils.replace(scriptContent, replacement[0], replacement[1]);
      }

      return scriptContent;
   }

   private void load() {
      if(isTemplateContentEmpty()) {

         synchronized (templateContent) {
            InputStream templateResStream = ScriptForUpdatingEtcHostsGenerator.class.getResourceAsStream(TEMPLATE_RESOURCE);

            if (templateResStream == null) {
               throw BddException.SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_NOT_FOUND();
            }

            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader templateBufReader = new BufferedReader(new InputStreamReader(templateResStream))) {
               String line = templateBufReader.readLine();
               while (line != null) {
                  stringBuilder.append(line).append('\n');
                  line = templateBufReader.readLine();
               }
            } catch (IOException ioe) {
               throw BddException.SCRIPT_FOR_UPDATING_ETC_HOSTS_TEMPLATE_READ_ERR(ioe, ioe.getMessage());
            }

            setTemplateContent(stringBuilder);
         }
      }
   }

   private boolean isTemplateContentEmpty() {
      return templateContent[0] == null;
   }

   private void setTemplateContent(StringBuilder content) {
      templateContent[0] = content;
   }

   private StringBuilder getTemplateContent() {
      return templateContent[0];
   }

}
