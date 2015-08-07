/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ibm.icu.text.SimpleDateFormat;

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "node_template_seq", allocationSize = 1)
@Table(name = "node_template")
public class NodeTemplateEntity extends EntityBase {

   @Column(name = "name")
   private String name;

   @Column(name = "moid")
   private String moid;

   /*
    * The last modified time of the node template VM
    */
   @Column(name = "last_modified")
   private Date lastModified;

   /*
    * The template tag which is used by the Instant Clone to determine the parent VM name
    */
   @Column(name = "tag")
   private String tag;

   @Column(name = "os_family")
   private String osFamily;

   @Column(name = "os_version")
   private String osVersion;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getMoid() {
      return moid;
   }

   public void setMoid(String moid) {
      this.moid = moid;
   }

   public String getTag() {
      if (tag == null && lastModified != null) {
         return new SimpleDateFormat("yyyyMMddHHmmss").format(lastModified);
      }
      return tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public String getOsFamily() {
      return osFamily;
   }

   public void setOsFamily(String osFamily) {
      this.osFamily = osFamily;
   }

   public String getOsVersion() {
      return osVersion;
   }

   public void setOsVersion(String osVersion) {
      this.osVersion = osVersion;
   }

   public Date getLastModified() {
      return lastModified;
   }

   public void setLastModified(Date lastModified) {
      this.lastModified = lastModified;
   }

}
