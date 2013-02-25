/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class ValidateResult {

   public enum Type {
      WHITE_LIST_NO_EXIST_FILE_NAME,
      WHITE_LIST_INVALID_NAME,
      WHITE_LIST_INVALID_VALUE,
      WHITE_LIST_INVALID_NAME_VALUE,
      NAME_IN_BLACK_LIST,
      VALID
   }

   private Type type = Type.VALID;
   private List<String> failureNames = new LinkedList<String>(); // configuration name list
   private List<String> failureValues = new LinkedList<String>(); // configuration value list
   private Map<String, List<String>> noExistFileNames = new HashMap<String, List<String>>();
   private Map<String,List<String>> blackPropertisMap = new HashMap<String, List<String>>(); // It utilize to store propertis which it in the blacklist (key:filename,value:property name list).

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public List<String> getFailureNames() {
      return failureNames;
   }

   public void setFailureNames(List<String> failureNames) {
      this.failureNames = failureNames;
   }

   public List<String> getFailureValues() {
      return failureValues;
   }

   public void setFailureValues(List<String> failureValues) {
      this.failureValues = failureValues;
   }

   public Map<String, List<String>> getNoExistFileNames() {
      return noExistFileNames;
   }

   public void setNoExistFileNames(Map<String, List<String>> noExistFileNames) {
      this.noExistFileNames = noExistFileNames;
   }

   public void addFailureName(String failureName) {
      this.failureNames.add(failureName);
      
   }

   public void addFailureValue(String failureValue) {
      this.failureValues.add(failureValue);
   }

   public void addNoExistFileName(String configType, String noExistFileName) {
      if (noExistFileNames != null) {
         List<String> fileNames = noExistFileNames.get(configType);
         if (fileNames == null) {
            fileNames = new LinkedList<String>();
         }
         fileNames.add(noExistFileName);
         noExistFileNames.put(configType, fileNames);
      }
   }

   public List<String> getNoExistFileNamesByConfigType(String configType) {
      List<String> fileNames = noExistFileNames.get(configType);
      if (fileNames != null) {
         return fileNames;
      } else {
         return new LinkedList<String>();
      }
   }

   public Map<String, List<String>> getBlackPropertisMap() {
      return blackPropertisMap;
   }

   public void setBlackPropertisMap(Map<String, List<String>> blackPropertisMap) {
      this.blackPropertisMap = blackPropertisMap;
   }

   public void putProperty(final String fileName, final String propertyName) {
      if (blackPropertisMap != null) {
         if (blackPropertisMap.containsKey(fileName)) {
            List<String> propertyList = blackPropertisMap.get(fileName);
            propertyList.add(propertyName);
         } else {
            List<String> propertyList = new LinkedList<String>();
            propertyList.add(propertyName);
            blackPropertisMap.put(fileName, propertyList);
         }
      }
   }

   public List<String> getProperty(final String fileName, final String propertyName) {
      if (blackPropertisMap != null && blackPropertisMap.containsKey(fileName)) {
         return blackPropertisMap.get(fileName);
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      Gson g=new Gson();
      return g.toJson(this);
   }
   
}
