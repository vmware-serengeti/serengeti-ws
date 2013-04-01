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

package com.vmware.aurora.composition;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import java.lang.reflect.Field;

/**
 * Class representing the DiskSchema
 *
 * @author sridharr
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "networkSchema")
public class NetworkSchema extends Schema {

   public static class Network extends Schema {
      @XmlAttribute()
      public String nicLabel;

      @XmlAttribute()
      public String vcNetwork;

      @Override
      protected Object fieldValueOf(Field field)
            throws IllegalArgumentException, IllegalAccessException {
         return field.get(this);
      }

   };

   @XmlAttribute()
   public String name;

   @XmlAttribute()
   public String parent;

   @XmlElementWrapper(name = "networks")
   @XmlElement(name = "network")
   public ArrayList<Network> networks;

   @Override
   protected Object fieldValueOf(Field field) throws IllegalArgumentException,
         IllegalAccessException {
      return field.get(this);
   }

}
