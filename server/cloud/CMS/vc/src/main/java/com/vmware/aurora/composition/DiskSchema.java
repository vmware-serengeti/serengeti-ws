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

import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

/**
 * Class representing the DiskSchema
 *
 * @author sridharr
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "diskSchema")
public class DiskSchema extends Schema {
   public enum DiskAttribute {
      PROMOTE
   }

   public static class Disk extends Schema {
      public enum Operation {
         CLONE,
         ADD,
         REMOVE,
         PROMOTE
      }
      @XmlAttribute()
      public String name;

      @XmlAttribute()
      public String type;

      @XmlAttribute()
      public AllocationType allocationType;

      @XmlAttribute()
      public String externalAddress;

      @XmlAttribute()
      public int initialSizeMB;

      @XmlAttribute()
      public DiskMode mode;

      @XmlAttribute()
      public String datastore;

      @XmlElementWrapper(name = "diskAttributes")
      @XmlElement(name = "diskAttribute")
      public ArrayList<DiskAttribute> attributes;

      @Override
      protected Object fieldValueOf(Field field)
            throws IllegalArgumentException, IllegalAccessException {
         return field.get(this);
      }
   };

   @XmlAttribute()
   private String name;

   @XmlElementWrapper(name = "disks")
   @XmlElement(name = "disk")
   private ArrayList<Disk> disks;

   @XmlAttribute()
   private String parent;

   @XmlAttribute()
   private String parentSnap;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void setDisks(ArrayList<Disk> disks) {
      this.disks = disks;
   }

   public ArrayList<Disk> getDisks() {
      return this.disks;
   }

   public void setParent(String parent) {
      this.parent = parent;
   }

   public String getParent() {
      return this.parent;
   }

   public void setParentSnap(String parentSnap) {
      this.parentSnap = parentSnap;
   }

   public String getParentSnap() {
      return this.parentSnap;
   }

   @Override
   protected Object fieldValueOf(Field field) throws IllegalArgumentException,
         IllegalAccessException {
      return field.get(this);
   }
}
