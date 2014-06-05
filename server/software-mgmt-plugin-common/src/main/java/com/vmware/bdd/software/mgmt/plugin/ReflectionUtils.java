package com.vmware.bdd.software.mgmt.plugin;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Xiaoding Bian
 * Date: 6/5/14
 * Time: 2:01 PM
 */
public class ReflectionUtils {

   private static final Class<?>[] EMPTY_ARRAY = new Class[]{};
   private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE =
         new ConcurrentHashMap<Class<?>, Constructor<?>>();

   @SuppressWarnings("unchecked")
   public static <T> T newInstance(Class<T> theClass, SoftwareProvider.Cache.PluginConf conf) {
      T result;
      try {
         Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
         if (meth == null) {
            meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
            meth.setAccessible(true);
            CONSTRUCTOR_CACHE.put(theClass, meth);
         }
         result = meth.newInstance(conf);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      return result;
   }
}
