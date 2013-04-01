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
package com.vmware.aurora.stats;

import java.util.concurrent.atomic.AtomicLong;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/*
 * To keep trace of causal relations between events,
 * a STATS entry is contains a source stats type and a destination stats type.
 * The source stats type defines the source or originator of the event.
 * The destination stats type represents the resulting operation or request
 * as a result of the source event.
 * The IDs of the stats entry identifies other unique aspects.
 */
public class StatsEntry implements Comparable<StatsEntry> {
   private final StatsType dest;
   private final StatsType src;
   private final String id;
   private final int hashcode;
   private volatile AtomicLong count; // current count
   private long lastCount;      // count obtained in the last interval

   private int calcHashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + dest.hashCode();
      result = prime * result + src.hashCode();
      result = prime * result + id.hashCode();
      return result;
   }

   private static final String objToString(Object o) {
      if (o instanceof String) {
         return (String)o;
      } else if (o instanceof Enum<?>) {
         return ((Enum<?>)o).toString();
      } else if (o instanceof Class<?>) {
         return ((Class<?>)o).getSimpleName();
      } else if (o instanceof ManagedObjectReference) {
         return ((ManagedObjectReference)o).getType();
      } else {
         return o.getClass().getSimpleName();
      }
   }

   private static final String convertId(final Object[] objs) {
      StringBuffer buf = new StringBuffer();
      for (Object o : objs) {
         buf.append('.').append(objToString(o));
      }
      return buf.toString();
   }

   /**
    * @return a unique String representation of a STATS entry
    */
   static final String getKey(final StatsType src, final StatsType dest,
         final Object... objs) {
      String srcName = ((src == null) ? StatsType.ROOT : src).name();
      StringBuffer buf = new StringBuffer(srcName);
      buf.append(dest.name()).append(convertId(objs));
      return buf.toString();
   }

   StatsEntry(final StatsType src, final StatsType dest, final Object... objs) {
      this.src = ((src == null) ? StatsType.ROOT : src);
      this.dest = dest;
      this.id = convertId(objs);
      this.hashcode = calcHashCode();
      this.count = new AtomicLong(1);
      this.lastCount = 0;
   }

   final void inc() {
      count.incrementAndGet();
   }

   final long getCount() {
      return count.longValue();
   }

   /**
    * Update the last count with new count.
    * @param newCount
    * @return the different from the last count.
    */
   final long updateLastCount(long newCount) {
      long diff = newCount - lastCount;
      lastCount = newCount;
      return diff;
   }

   final StatsType getSrc() {
      return src;
   }

   final StatsType getDest() {
      return dest;
   }

   @Override
   public final int hashCode() {
      return hashcode;
   }

   @Override
   public final boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof StatsEntry)) {
         return false;
      }
      StatsEntry other = (StatsEntry) obj;
      return (dest == other.dest &&
              src == other.src &&
              id.equals(other.id));
   }

   @Override
   public final int compareTo(StatsEntry o) {
      int val = dest.compareTo(o.dest);
      if (val == 0) {
         val = id.compareTo(o.id);
      }
      if (val == 0) {
         val = src.compareTo(o.src);
      }
      return val;
   }

   @Override
   public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(dest.toString()).append(id)
         .append('<').append(src.toString()).append('>');
      return buf.toString();
   }
}
