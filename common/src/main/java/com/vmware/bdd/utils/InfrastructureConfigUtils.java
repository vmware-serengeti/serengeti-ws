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
package com.vmware.bdd.utils;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vmware.bdd.exception.ClusterConfigException;

/**
 * Created By xiaoliangl on 2/12/15.
 */
public class InfrastructureConfigUtils {
   public static Map<String, Map<String, String>> read(String json) {
      ObjectMapper objectMapper = new ObjectMapper();

      try {
         return objectMapper.readValue(json, Map.class);
      } catch (IOException e) {
         throw ClusterConfigException.INVALID_INFRA_CONFIG(e);
      }

   }

   public static String write(Map<String, Map<String, String>> infraConfig) {
      ObjectMapper objectMapper = new ObjectMapper();

      try {
         return objectMapper.writeValueAsString(infraConfig);
      } catch (JsonProcessingException e) {
         throw ClusterConfigException.INVALID_INFRA_CONFIG(e);
      }

   }
}
