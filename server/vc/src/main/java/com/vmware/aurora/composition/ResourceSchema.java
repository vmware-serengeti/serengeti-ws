/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.composition;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import java.lang.reflect.Field;

import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.aurora.interfaces.model.IDatabaseConfig.Priority;

/**
 * Class representing the resource (CPU, memory) schema
 *
 * @author sridharr
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resourceSchema")
public class ResourceSchema extends Schema {

   @XmlAttribute
   public String name;

   @XmlAttribute(required = true)
   public int numCPUs;

   @XmlAttribute(required = true)
   public long cpuReservationMHz;

   @XmlAttribute(required = true)
   public long memSize;

   @XmlAttribute(required = true)
   public long memReservationSize;

   @XmlAttribute(required = true)
   public Priority priority;

   @XmlAttribute()
   public LatencyPriority latencySensitivity;

   @Override
   protected Object fieldValueOf(Field field) throws IllegalArgumentException,
         IllegalAccessException {
      return field.get(this);
   }
}
