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
package com.vmware.bdd.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;
import com.vmware.bdd.utils.ValidateResult.Type;

public class AppConfigValidationFactory {

    static final Logger logger = Logger.getLogger(AppConfigValidationFactory.class);

    @SuppressWarnings("unchecked")
    public static ValidateResult blackListHandle(Map<String, Object> config) {
        ValidateResult validateResult = new ValidateResult();
        String jsonStr = CommonUtil.readJsonFile("blacklist.json");
        Gson gson = new Gson();
        List<Map<String, List<String>>> blackList = gson.fromJson(jsonStr, List.class);
        for (Entry<String, Object> configType : config.entrySet()) {
            if (((String) configType.getKey()).trim().equalsIgnoreCase("hadoop")) {
                if (!(configType.getValue() instanceof Map)) {
                    throw new RuntimeException(Constants.CLUSTER_CONFIG_ERROR);
                }
                Map<String, Object> propertyConfig = (Map<String, Object>) (configType.getValue());
                String configFileName = "";
                for (Entry<String, Object> configFileEntry : propertyConfig.entrySet()) {
                    configFileName = configFileEntry.getKey();
                    validateBySameFileName(configFileName, configFileEntry.getValue(), blackList, validateResult,
                            ValidationType.BLACK_LIST);
                }
            }
        }
        return validateResult;
    }

    @SuppressWarnings("unchecked")
    public static ValidateResult whiteListHandle(Map<String, Object> config) {
        ValidateResult validateResult = new ValidateResult();
        String jsonStr = CommonUtil.readJsonFile("whitelist.json");
        Gson gson = new Gson();
        List<Map<String, List<Map<String, String>>>> whiteList = gson.fromJson(jsonStr, List.class);
        for (Entry<String, Object> configType : config.entrySet()) {
            if (((String) configType.getKey()).trim().equalsIgnoreCase("hadoop")) {
                if (!(configType.getValue() instanceof Map)) {
                    throw new RuntimeException(Constants.CLUSTER_CONFIG_ERROR);
                }
                Map<String, Object> propertyConfig = (Map<String, Object>) (configType.getValue());
                String configFileName = "";
                for (Entry<String, Object> configFileEntry : propertyConfig.entrySet()) {
                    configFileName = configFileEntry.getKey();
                    validateBySameFileName(configFileName, configFileEntry.getValue(), whiteList, validateResult,
                            ValidationType.WHITE_LIST);
                    if (validateResult.getType() == Type.VALID) {
                       valdiateSpecialFileFormat(configFileName, configFileEntry.getValue(), validateResult);
                    }
                }
            }
        }
        return validateResult;
    }

//    process non key-value xml files such as fair-scheduler.xml below
//    <?xml version="1.0"?>
//    <allocations>
//      <pool name="sample_pool">
//        <minMaps>5</minMaps>
//        <minReduces>5</minReduces>
//        <weight>2.0</weight>
//      </pool>
//      <user name="sample_user">
//        <maxRunningJobs>6</maxRunningJobs>
//      </user>
//      <userMaxJobsDefault>3</userMaxJobsDefault>
//    </allocations>
    @SuppressWarnings("unchecked")
   private static void valdiateSpecialFileFormat(String configFileName,
         Object configProperties, ValidateResult validateResult) {
       if (configFileName.equals(Constants.FAIR_SCHEDULER_FILE_NAME)) {
          Map<String, Object> configPropertyMap = (Map<String, Object>) configProperties;
          String xmlContents = (String)configPropertyMap.get(Constants.FAIR_SCHEDULER_FILE_ATTRIBUTE);
          checkFairSchedulerXmlFormat(xmlContents, validateResult);
       }
   }

   private static void checkFairSchedulerXmlFormat(String xmlContents, ValidateResult validateResult) {
      //slightly modified hadoop codes to check fair-scheduler.xml format
      //https://github.com/apache/hadoop/blob/trunk/src/contrib/fairscheduler/src/java/org/apache/hadoop/mapred/PoolManager.java
      DocumentBuilderFactory docBuilderFactory =
            DocumentBuilderFactory.newInstance();
      docBuilderFactory.setIgnoringComments(true);
      DocumentBuilder builder = null;
      Document doc = null;
      try {
         builder = docBuilderFactory.newDocumentBuilder();
         doc = builder.parse(xmlContents);
      } catch (Exception e) {
         validateResult.addFailureName(Constants.FAIR_SCHEDULER_FILE_ATTRIBUTE);
         return;
      }
      Element root = doc.getDocumentElement();
      if (!"allocations".equals(root.getTagName())) {
         validateResult.addFailureName(root.getTagName());
         return;
      }
      NodeList elements = root.getChildNodes();
      for (int i = 0; i < elements.getLength(); i++) {
         Node node = elements.item(i);
         if (!(node instanceof Element))
            continue;
         Element element = (Element)node;
         if ("pool".equals(element.getTagName())) {
            NodeList fields = element.getChildNodes();
            for (int j = 0; j < fields.getLength(); j++) {
               Node fieldNode = fields.item(j);
               if (!(fieldNode instanceof Element))
                  continue;
               Element field = (Element) fieldNode;
               String text = null;
               try {
                  if ("minMaps".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     int val = Integer.parseInt(text);
                  } else if ("minReduces".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     int val = Integer.parseInt(text);
                  } else if ("maxRunningJobs".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     int val = Integer.parseInt(text);
                  } else if ("weight".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     double val = Double.parseDouble(text);
                  } else if ("maxMaps".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     int val = Integer.parseInt(text);
                  } else if ("maxReduces".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     int val = Integer.parseInt(text);
                  } else if ("minSharePreemptionTimeout".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                     long val = Long.parseLong(text) * 1000L;
                  } else if ("schedulingMode".equals(field.getTagName())) {
                     text = ((Text)field.getFirstChild()).getData().trim();
                  }
               }catch (NumberFormatException e) {
                  validateResult.addFailureValue(text);
               }
            }
         } else if ("user".equals(element.getTagName())) {
            NodeList fields = element.getChildNodes();
            for (int j = 0; j < fields.getLength(); j++) {
               Node fieldNode = fields.item(j);
               if (!(fieldNode instanceof Element))
                  continue;
               Element field = (Element) fieldNode;
               if ("maxRunningJobs".equals(field.getTagName())) {
                  String text = ((Text)field.getFirstChild()).getData().trim();
                  try {
                     int val = Integer.parseInt(text);
                  } catch (NumberFormatException e) {
                     validateResult.addFailureValue(text);
                  }       
               }
            }
         } else if ("userMaxJobsDefault".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
            try {
               int val = Integer.parseInt(text);
            } catch (NumberFormatException e) {
               validateResult.addFailureValue(text);
            }  
         } else if ("poolMaxJobsDefault".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
            try {
               int val = Integer.parseInt(text);
            } catch (NumberFormatException e) {
               validateResult.addFailureValue(text);
            }
         } else if ("poolMaxJobsDefault".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
            try {
               int val = Integer.parseInt(text);
            } catch (NumberFormatException e) {
               validateResult.addFailureValue(text);
            }
         } else if ("fairSharePreemptionTimeout".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
            try {
               long val = Integer.parseInt(text) * 1000L;
            } catch (NumberFormatException e) {
               validateResult.addFailureValue(text);
            }
         } else if ("defaultMinSharePreemptionTimeout".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
            try {
               long val = Integer.parseInt(text) * 1000L;
            } catch (NumberFormatException e) {
               validateResult.addFailureValue(text);
            }
         } else if ("defaultPoolSchedulingMode".equals(element.getTagName())) {
            String text = ((Text)element.getFirstChild()).getData().trim();
         } else {
            validateResult.addFailureName(element.getTagName());
         }
      } 
}

   @SuppressWarnings("unchecked")
    private static <T> void validateBySameFileName(String fileName, Object configProperties,
            List<Map<String, T>> warnPropertyList, ValidateResult validateResult, ValidationType validationType) {
        for (Map<String, T> warnPropertyFileMap : warnPropertyList) {
            if (warnPropertyFileMap.containsKey(fileName) && configProperties instanceof Map) {
                Map<String, Object> configPropertyMap = (Map<String, Object>) configProperties;
                List<String> removeList=new ArrayList<String>();
                for (Entry<String, Object> configProperty : configPropertyMap.entrySet()) {
                    if (validationType == ValidationType.WHITE_LIST) {
                        for (Entry<String, T> warnPropertyFileEntry : warnPropertyFileMap.entrySet()) {
                            if (warnPropertyFileEntry.getValue() instanceof List) {
                                List<Object> propertyList = (List<Object>) warnPropertyFileEntry.getValue();
                                if (!validateWhiteListPropertis(propertyList, configProperty.getKey(),
                                        String.valueOf(configProperty.getValue()), validateResult)) {
                                }
                            }
                        }
                    } else if (validationType == ValidationType.BLACK_LIST) {
                        for (Entry<String, T> warnPropertyFileEntry : warnPropertyFileMap.entrySet()) {
                            if (warnPropertyFileEntry.getValue() instanceof List) {
                                List<String> propertyList = (List<String>) warnPropertyFileEntry.getValue();
                                for (String propertyName : propertyList) {
                                    if (configProperty.getKey().equals(propertyName)) {
                                        validateResult.setType(ValidateResult.Type.NAME_IN_BLACK_LIST);
                                        validateResult.addFailureName(configProperty.getKey());
                                        validateResult.putProperty(fileName, propertyName);
                                        removeList.add(propertyName);
                                    }
                                }
                            }
                        }
                    }
                }
                //remove black property from configuration
                for(String pName:removeList){
                   configPropertyMap.remove(pName);
                }
            }
        }
    }

    private static boolean validatePropertyValueFormat(final String value, final String format) {
        //TODO
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean validateWhiteListPropertis(List<Object> propertyList, String configPropertyName,
            String configPropertyValue, ValidateResult validateResult) {
        ValidateResult.Type validateType = ValidateResult.Type.WHITE_LIST_INVALID_NAME;
        for (Object obj : propertyList) {
            if (obj instanceof Map) {
                Map<String, String> property = (Map<String, String>) obj;

                if ((property.get("nameIsPattern") != null && property.get("nameIsPattern").trim().equalsIgnoreCase("true") && configPropertyName.matches(property.get("name")))
                      || property.get("name").trim().equalsIgnoreCase(configPropertyName)) {
                    if (property.get("valueFormat") != null && !property.get("valueFormat").isEmpty()
                            && !validatePropertyValueFormat(configPropertyValue, property.get("valueFormat"))) {
                        validateType = ValidateResult.Type.WHITE_LIST_INVALID_VALUE;
                    }
                    validateType = ValidateResult.Type.VALID;
                }
            }
        }
        if (validateType == ValidateResult.Type.WHITE_LIST_INVALID_NAME) {
            validateResult.addFailureName(configPropertyName);
            if (validateResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_VALUE
                    || validateResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME_VALUE) {
                validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_NAME_VALUE);
            } else {
                validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_NAME);
            }
            return false;
        } else if (validateType == ValidateResult.Type.WHITE_LIST_INVALID_VALUE) {
            validateResult.addFailureName(configPropertyValue);
            if (validateResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME
                    || validateResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME_VALUE) {
                validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_NAME_VALUE);
            } else {
                validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
            }
            return false;
        }
        return true;
    }
}
