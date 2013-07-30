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
package com.vmware.aurora.global;

@SuppressWarnings("serial")
public class DiskSize extends Size {
   /*
    * In Aurora, disk unit uses 2^n instead of decimal
    * conversion as defined by ISO.
    *
    * However, the code should generally follow ISO naming that
    * memory size uses "KiB/MiB/GiB" and disk size uses "KB/MB/GB".
    */
   public static final long KB = (1L << 10);
   public static final long MB = (1L << 20);
   public static final long GB = (1L << 30);

   public DiskSize(long size) {
      super(size);
   }

   public DiskSize(Size size) {
      this(size.getSize());
   }

   public long getKB() {
      return sizeInBytes / KB;
   }

   public float getKBFloat() {
      return (float)sizeInBytes / KB;
   }

   public long getMB() {
      return sizeInBytes / MB;
   }

   public float getMBFloat() {
      return (float)sizeInBytes / MB;
   }

   public long getGB() {
      return sizeInBytes / GB;
   }

   public float getGBFloat() {
      return (float)sizeInBytes / GB;
   }

   /**
    * Conversion from memory unit.
    * @param sizeKiB
    * @return
    */
   static public DiskSize sizeFromKiB(long sizeKiB) {
      return new DiskSize(sizeKiB << 10);
   }

   /**
    * Conversion from memory unit.
    * @param sizeMiB
    * @return
    */
   static public DiskSize sizeFromMiB(long sizeMiB) {
      return new DiskSize(sizeMiB << 20);
   }

   static public DiskSize sizeFromKB(long sizeKB) {
      return new DiskSize(sizeKB * KB);
   }

   static public DiskSize sizeFromMB(long sizeMB) {
      return new DiskSize(sizeMB * MB);
   }

   static public DiskSize sizeFromGB(long sizeGB) {
      return new DiskSize(sizeGB * GB);
   }

   public DiskSize add(DiskSize size) {
      sizeInBytes = addOp(size.getSize());
      return this;
   }

   public DiskSize add(long ... sizes) {
      sizeInBytes = addOp(sizes);
      return this;
   }

   public DiskSize add(DiskSize ... sizes) {
      sizeInBytes = addOp(sizes);
      return this;
   }

   public DiskSize sub(DiskSize size) {
      sizeInBytes = subOp(size.getSize());
      return this;
   }

   public DiskSize sub(long ... sizes) {
      sizeInBytes = subOp(sizes);
      return this;
   }

   public DiskSize sub(DiskSize ... sizes) {
      sizeInBytes = subOp(sizes);
      return this;
   }

   public DiskSize neg() {
      sizeInBytes = negOp();
      return this;
   }
}
