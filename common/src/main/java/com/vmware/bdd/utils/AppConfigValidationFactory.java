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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;

public class AppConfigValidationFactory {

    static final Logger logger = Logger.getLogger(AppConfigValidationFactory.class);

    @SuppressWarnings("unchecked")
    public static ValidateResult blackListHandle(Map<String, Object> config) {
        ValidateResult validateResult = new ValidateResult();
        String jsonStr = readJsonFile("blacklist.json");
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
        String jsonStr = readJsonFile("whitelist.json");
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
                }
            }
        }
        return validateResult;
    }

    @SuppressWarnings("unchecked")
    private static <T> void validateBySameFileName(String fileName, Object configProperties,
            List<Map<String, T>> warnPropertyList, ValidateResult validateResult, ValidationType validationType) {
        for (Map<String, T> warnPropertyFileMap : warnPropertyList) {
            if (warnPropertyFileMap.containsKey(fileName) && configProperties instanceof Map) {
                Map<String, Object> configPropertyMap = (Map<String, Object>) configProperties;
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
                                    }
                                }
                            }
                        }
                    }
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

                if (property.get("name").trim().equalsIgnoreCase(configPropertyName)) {
                    if (property.get("valueFormat")!=null && !property.get("valueFormat").isEmpty()
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

    private static String readJsonFile(final String fileName) {
        StringBuilder jsonBuff = new StringBuilder();
        URL fileURL = AppConfigValidationUtils.class.getClassLoader().getResource(fileName);
        if (fileURL != null) {
            InputStream in = null;
            try {
                in = new BufferedInputStream(fileURL.openStream());
                Reader rd = new InputStreamReader(in, "UTF-8");
                int c = 0;
                while ((c = rd.read()) != -1) {
                    jsonBuff.append((char) c);
                }
            } catch (IOException e) {
                logger.error(e.getMessage() + "\n Can not find " + fileName + " or IO read error.");
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e.getMessage() + "\n Can not close " + fileName + ".");
                }
            }
        }
        return jsonBuff.toString();
    }
}
