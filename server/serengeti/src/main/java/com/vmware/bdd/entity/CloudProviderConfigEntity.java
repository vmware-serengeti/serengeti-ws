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
package com.vmware.bdd.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.vmware.bdd.dal.DAL;

/**
 * Work as a message queue
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "cloud_provider_config_seq", allocationSize = 1)
@Table(name = "cloud_provider_config")
public class CloudProviderConfigEntity extends EntityBase {

   @Column(name = "cloud_type", nullable = false)
   private String cloudType;

   @Column(name = "attribute", nullable = false)
   private String attribute;

   @Column(name = "value")
   private String value;

   public String getCloudType() {
      return cloudType;
   }

   public void setCloudType(String cloudType) {
      this.cloudType = cloudType;
   }

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(String attribute) {
      this.attribute = attribute;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public static List<CloudProviderConfigEntity> findAllByType(String type) {
      Criterion typeEquals = Restrictions.eq("cloudType", type);
      return DAL.findByCriteria(CloudProviderConfigEntity.class, typeEquals);
   }
   
   public static CloudProviderConfigEntity findUniqueByAttributeValue(String type, String attributeName, String attributeValue) {
      Map<String, String> propertyNameValues = new HashMap<String, String>();
      propertyNameValues.put("cloudType", type);
      propertyNameValues.put("attribute", attributeName);
      propertyNameValues.put("value", attributeValue);
      return DAL.findUniqueByCriteria(CloudProviderConfigEntity.class, Restrictions.allEq(propertyNameValues));
   }
}
