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
package com.vmware.bdd.software.mgmt.plugin.utils;

import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Xiaoding Bian
 * Date: 7/29/14
 * Time: 10:56 AM
 */
public class ReflectionUtils {

   private static final Logger logger = Logger.getLogger(ReflectionUtils.class);

   private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE =
         new ConcurrentHashMap<Class<?>, Constructor<?>>();

   private static final Class<?>[] EMPTY_ARRAY = new Class[]{};

   public static PreStartServices getPreStartServicesHook() {
      String className = "com.vmware.bdd.aop.software.DefaultPreStartServicesAdvice";
      Class<? extends PreStartServices> clazz = getClass(className, PreStartServices.class);
      return newInstance(clazz);
   }

   public static <U> Class<? extends U> getClass(String className, Class<U> superClass) {
      try {
         Class<?> theClass = Class.forName(className);
         if (theClass != null && !superClass.isAssignableFrom(theClass)) {
            throw new RuntimeException(theClass + " is not a subclass of " + superClass.getName());
         } else if (theClass != null) {
            return theClass.asSubclass(superClass);
         } else {
            return null;
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> T newInstance(Class<T> theClass) {
      T result;
      try {
         Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
         if (meth == null) {
            meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
            meth.setAccessible(true);
            CONSTRUCTOR_CACHE.put(theClass, meth);
         }
         result = meth.newInstance();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      return result;
   }

   /**
    * TODO: launch a daemon thread to call this function to avoid potential memory leak
    */
   static void clearCache() {
      CONSTRUCTOR_CACHE.clear();
   }

   static int getCacheSize() {
      return CONSTRUCTOR_CACHE.size();
   }
}
