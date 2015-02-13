/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.util.collection;

import com.google.gson.Gson;
import com.vmware.bdd.utils.ByteArrayUtils;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SensitiveDataObfuscator {

    static final Logger logger = Logger.getLogger(SensitiveDataObfuscator.class);

    private static List<String> sensitiveData = new ArrayList<>();

    static {
        sensitiveData = getSensitiveDataFromFile();
    }

    public static List<String> getSensitiveDataFromFile() {
        URL url = SensitiveDataObfuscator.class.getResource(CollectionConstants.SENSITIVE_DATA_FILE);
        String sensitiveData = CommonUtil.readJsonFile(url);
        Gson gson = new Gson();
        return gson.fromJson(sensitiveData, List.class);
    }

    public static String hashSensitiveDataHierarchically(String key, String value, List<String> expandedSensitiveData) {
        String newKey = key;
        if (!CommonUtil.isBlank(key) && (key.indexOf(".") != -1)) {
            String hashValue = CommonUtil.notNull(hashSensitiveData(newKey, value, expandedSensitiveData), "");
            if (!CommonUtil.isBlank(hashValue) && !hashValue.equals(value)) {
                return hashValue;
            }
            String[] words = key.split("\\.");
            if (words.length > 0) {
                int j = 1;
                int index = 0;
                hashValue = "";
                while ((index = words.length - j) >= 0) {
                    newKey = words[index];
                    hashValue = hashSensitiveData(newKey, value, expandedSensitiveData);
                    if (hashValue.equals(value)) {
                        j++;
                    } else  {
                        return hashValue;
                    }
                }
            }
            return value;
        } else {
            return hashSensitiveData(newKey, value, expandedSensitiveData);
        }
    }

    public static String hashSensitiveData(String key, String value, List<String> expandedSensitiveData) {
        if ((sensitiveData != null && sensitiveData.contains(key)) ||
                (expandedSensitiveData != null &&
                        expandedSensitiveData.contains(key)) && !CommonUtil.isBlank(value)) {
             return parseStrToMd5U32(value);
        } else {
            return value;
        }
    }

    public static String parseStrToMd5U32(String str){
        String reStr = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            reStr = ByteArrayUtils.byteArrayToHexString(bytes);
            } catch (NoSuchAlgorithmException e) {
                logger.warn("Hash sensitive data failed: " + e.getMessage());
            }
            return reStr;
        }
}
