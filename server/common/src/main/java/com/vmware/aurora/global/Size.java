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

import java.io.Serializable;

@SuppressWarnings("serial")
public class Size implements Serializable {
   protected long sizeInBytes;

   protected Size(long sizeInBytes) {
      this.sizeInBytes = sizeInBytes;
   }

   public long getSize() {
      return sizeInBytes;
   }

   public float getFloat() {
      return (float)sizeInBytes;
   }

   public long getKiB() {
      return sizeInBytes >> 10;
   }

   public float getKiBFloat() {
      return (float)sizeInBytes / (1 << 10);
   }

   public long getMiB() {
      return sizeInBytes >> 20;
   }

   public float getMiBFloat() {
      return (float)sizeInBytes / (1 << 20);
   }

   public long getGiB() {
      return sizeInBytes >> 30;
   }

   public float getGiBFloat() {
      return (float)sizeInBytes / (1 << 30);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Size) {
         return sizeInBytes == ((Size)obj).sizeInBytes;
      }
      return false;
   }
   @Override
   public int hashCode() {
      return new Long(sizeInBytes).hashCode();
   }

   protected long addOp(long size) {
      return sizeInBytes + size;
   }

   protected long addOp(long ... sizes) {
      long size = sizeInBytes;
      for (int i = 0; i < sizes.length; i++) {
         size += sizes[i];
      }
      return size;
   }

   protected long addOp(Size ... sizes) {
      long size = sizeInBytes;
      for (int i = 0; i < sizes.length; i++) {
         size += sizes[i].sizeInBytes;
      }
      return size;
   }

   protected long subOp(long size) {
      return sizeInBytes - size;
   }

   protected long subOp(long ... sizes) {
      long size = sizeInBytes;
      for (int i = 0; i < sizes.length; i++) {
         size -= sizes[i];
      }
      return size;
   }

   protected long subOp(Size ... sizes) {
      long size = sizeInBytes;
      for (int i = 0; i < sizes.length; i++) {
         size -= sizes[i].sizeInBytes;
      }
      return size;
   }

   protected long negOp() {
      return -sizeInBytes;
   }
}
