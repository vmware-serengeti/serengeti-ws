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
package com.vmware.bdd.manager.collection;

import java.util.Map;

public class FakeCollectionDriver implements CollectionDriver{

   @Override
   public void send(Map<String, Map<String, ?>> data) {

   }

   @Override
   public String getConfigurationFilePath() {
      return "/tmp/test.properties";
   }

   @Override
   public String getCronExpressionName() {
      return "serengeti.ph.cron.expression";
   }

   @Override
   public String getDefaultCronExpression() {
      return "0 0 1 ? * L";
   }

   @Override
   public String getCollectionSwitchName() {
      return "serengeti.ph.enable";
   }

   @Override
   public void setInstanceId(String instanceId) {

   }

}
