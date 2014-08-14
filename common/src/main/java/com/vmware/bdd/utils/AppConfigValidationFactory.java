/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;

public class AppConfigValidationFactory {

   static final Logger logger = Logger.getLogger(AppConfigValidationFactory.class);

   /*
    * Validate the config type if valid or not. Config type is a first nesting level in a configuration,
    * such as 'hadoop','hbase','zookeeper' etc.
    */
   @SuppressWarnings("unchecked")
   public static void validateConfigType(Map<String, Object> config, List<String> warningMsgList) {
      String jsonStr = CommonUtil.readJsonFile("whitelist.json");
      Gson gson = new Gson();
      List<Map<String, Map<String,List<Map<String, String>>>>> whiteList = gson.fromJson(jsonStr, List.class);
      validateConfigType(config, whiteList, warningMsgList);
   }

   @SuppressWarnings("unchecked")
   public static ValidateResult blackListHandle(Map<String, Object> config) {
      ValidateResult validateResult = new ValidateResult();
      String jsonStr = CommonUtil.readJsonFile("blacklist.json");
      Gson gson = new Gson();
      List<Map<String, Map<String, List<String>>>> blackList = gson.fromJson(jsonStr, List.class);
      return processAppConfigValidation(config, validateResult, blackList, ValidationType.BLACK_LIST);
   }

    @SuppressWarnings("unchecked")
    public static ValidateResult whiteListHandle(Map<String, Object> config) {
        ValidateResult validateResult = new ValidateResult();
        String jsonStr = CommonUtil.readJsonFile("whitelist.json");
        Gson gson = new Gson();
        List<Map<String, Map<String,List<Map<String, String>>>>> whiteList = gson.fromJson(jsonStr, List.class);
        return processAppConfigValidation(config,validateResult,whiteList,ValidationType.WHITE_LIST);
    }

   /**
    * Validate configure files of each config type.
    *
    * @param config
    * @param validateResult
    * @param list
    * @param type
    * @param <T>
    * @return
    */
   @SuppressWarnings("unchecked")
   private static <T> ValidateResult processAppConfigValidation(Map<String, Object> config,
         ValidateResult validateResult, List<Map<String, Map<String, List<T>>>> list, ValidationType type) {
      for (Entry<String, Object> configTypeEntry : config.entrySet()) {
         if (!(configTypeEntry.getValue() instanceof Map)) {
            throw new RuntimeException(Constants.CLUSTER_CONFIG_FORMAT_ERROR);
         }
         Map<String, Object> propertyConfig = (Map<String, Object>) (configTypeEntry.getValue());
         String configFileName = "";
         for (Entry<String, Object> configFileEntry : propertyConfig.entrySet()) {
            configFileName = configFileEntry.getKey();
            if (!validateConfigFileName(configTypeEntry.getKey(), configFileName, configFileEntry,
                  list, type, validateResult)) {
               continue;
            }
            if (type.equals(ValidationType.WHITE_LIST) && configFileName.equals(Constants.FAIR_SCHEDULER_FILE_NAME)) {
               valdiateSpecialFileFormat(configFileName, configFileEntry.getValue(), validateResult);
            }
         }
      }
      return validateResult;
   }

   private static <T> void validateConfigType(Map<String, Object> config,
         List<Map<String, Map<String, List<T>>>> list,
         List<String> warningMsgList) {
      if ((config.size() > 0) && (warningMsgList != null)) {
         List<String> grayList = new ArrayList<String>();
         for (String configType : config.keySet()) {
            boolean found = false;
            for (Map<String, Map<String, List<T>>> listTypeMap : list) {
               if (listTypeMap.containsKey(configType)) {
                  found = true;
               }
            }
            if (!found) {
               grayList.add(configType);
            }
         }

         if (grayList.size() > 0) {
            String formatStr = grayList.size() > 1 ? Constants.CLUSTER_CONFIG_TYPES_NOT_REGULAR : Constants.CLUSTER_CONFIG_TYPE_NOT_REGULAR;
            warningMsgList.add(
               String.format(formatStr, new ListToStringConverter(grayList, ','))
            );
         }
      }
   }

   private static <T> boolean validateConfigFileName(String configType, final String configFileName,Entry<String, Object> configFileEntry,
         List<Map<String, Map<String, List<T>>>> list, ValidationType type, ValidateResult validateResult) {
      for (Map<String, Map<String, List<T>>> listTypeMap : list) {
         if (listTypeMap.containsKey(configType)) {
            if (!(listTypeMap.get(configType) instanceof Map)) {
               throw new RuntimeException(Constants.LIST_CONFIG_ERROR);
            }
            Map<String, List<T>> listFileMap = listTypeMap.get(configType);
            if (!listFileMap.containsKey(configFileName)) {
               if (type.equals(ValidationType.WHITE_LIST)) {
                  if (validateResult.getType().equals(ValidateResult.Type.VALID)) {
                     validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_NAME);
                  }
                  if (!validateResult.getNoExistFileNamesByConfigType(configType).contains(configFileName)) {
                     validateResult.addNoExistFileName(configType, configFileName);
                  }
               }
               return false;
            }
            validateBySameFileName(configFileName, configFileEntry.getValue(), listFileMap, validateResult, type);
         }
      }
      return true;
   }

   /*
    * process non key-value xml files such as fair-scheduler.xml below
    * <?xml version="1.0"?>
    *   <allocations>
    *     <pool name="sample_pool">
    *       <minMaps>5</minMaps>
    *       <minReduces>5</minReduces>
    *       <weight>2.0</weight>
    *     </pool>
    *     <user name="sample_user">
    *       <maxRunningJobs>6</maxRunningJobs>
    *     </user>
    *    <userMaxJobsDefault>3</userMaxJobsDefault>
    *  </allocations>
    */
    @SuppressWarnings("unchecked")
   private static void valdiateSpecialFileFormat(String configFileName,
         Object configProperties, ValidateResult validateResult) {
       if (configFileName.equals(Constants.FAIR_SCHEDULER_FILE_NAME)) {
          Map<String, Object> configPropertyMap = (Map<String, Object>) configProperties;
          String xmlContents = (String)configPropertyMap.get(Constants.FAIR_SCHEDULER_FILE_ATTRIBUTE);
          if (xmlContents != null) {
             checkFairSchedulerXmlFormat(xmlContents, validateResult);
             if (!validateResult.getFailureValues().isEmpty()) {
                validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
             }
          }
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
         InputSource is = new InputSource(new StringReader(xmlContents));
         doc = builder.parse(is);
      } catch (Exception e) {
         validateResult.addFailureValue(xmlContents);
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
   private static <T> void validateBySameFileName(final String fileName, Object configProperties,
         Map<String, List<T>> listFileMap, ValidateResult validateResult, ValidationType validationType) {
      if (configProperties instanceof Map) {
         Map<String, Object> configPropertyMap = (Map<String, Object>) configProperties;
         List<String> removeList = new ArrayList<String>();
         for (Entry<String, Object> configProperty : configPropertyMap.entrySet()) {
            for (Entry<String, List<T>> listFileEntry : listFileMap.entrySet()) {
               if (listFileEntry.getKey().equals(fileName) && listFileEntry.getValue() instanceof List) {
                  List<T> propertiesPerListFile = (List<T>) listFileEntry.getValue();
                  if (validationType == ValidationType.BLACK_LIST) {
                     validateBlackListPropertis(fileName, propertiesPerListFile, configProperty.getKey(),
                           validateResult, removeList);
                  } else if (validationType == ValidationType.WHITE_LIST) {
                     validateWhiteListPropertis(propertiesPerListFile, configProperty.getKey(),
                           String.valueOf(configProperty.getValue()), validateResult);
                  }
               }
            }
         }
         //remove black property from configuration
         for (String pName : removeList) {
            configPropertyMap.remove(pName);
         }
      }
   }

    private static boolean validatePropertyValueFormat(final String value, final String format) {
       //TODO
        return true;
    }

    private static <T> void validateBlackListPropertis(final String fileName, List<T> propertiesPerListFile, String configPropertyName,
          ValidateResult validateResult, List<String> removeList) {
       for (T propertyName : propertiesPerListFile) {
          if ((propertyName instanceof String) && (configPropertyName.equals((String)propertyName))) {
             validateResult.setType(ValidateResult.Type.NAME_IN_BLACK_LIST);
             validateResult.addFailureName(configPropertyName);
             validateResult.putProperty(fileName, (String)propertyName);
             removeList.add((String)propertyName);
          }
       }
    }

    @SuppressWarnings("unchecked")
    private static <T> void validateWhiteListPropertis(List<T> propertiesPerListFile, String configPropertyName,
            String configPropertyValue, ValidateResult validateResult) {
        ValidateResult.Type validateType = ValidateResult.Type.WHITE_LIST_INVALID_NAME;
        for (T t : propertiesPerListFile) {
            if (t instanceof Map) {
                Map<String, String> property = (Map<String, String>) t;
                if ((property.get("nameIsPattern") != null && property.get("nameIsPattern").
                      trim().equalsIgnoreCase("true") && configPropertyName.matches(property.get("name")))
                      || property.get("name").trim().equalsIgnoreCase(configPropertyName)) {
                    if (property.get("valueFormat") != null && !property.get("valueFormat").isEmpty()
                            && !validatePropertyValueFormat(configPropertyValue, property.get("valueFormat"))) {
                        validateType = ValidateResult.Type.WHITE_LIST_INVALID_VALUE;
                    }
                    validateType = ValidateResult.Type.VALID;
                }
            }
        }

        //we will throw failure for invalid values, and throw warning for invalid names, so
        //invalid value has higher priority in the type.
        if (validateType == ValidateResult.Type.WHITE_LIST_INVALID_NAME) {
           if(!validateResult.getFailureNames().contains(configPropertyName)){
              validateResult.addFailureName(configPropertyName);
           }
           if (validateResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_VALUE) {
              validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
           } else {
              validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_NAME);
           }
        } else if (validateType == ValidateResult.Type.WHITE_LIST_INVALID_VALUE) {
           if(!validateResult.getFailureValues().contains(configPropertyValue)) {
              validateResult.addFailureValue(configPropertyValue);
           }
           validateResult.setType(ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
        }
    }
}
