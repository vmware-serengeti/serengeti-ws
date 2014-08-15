package com.vmware.bdd.utils;

import java.util.List;

/**
 * convert a list to a string separated by a char.
 */
public class ListToStringConverter<T> {
   private final String str;

   public ListToStringConverter(List<T> list, char separator) {
      int size = list.size();
      int count = 0;
      StringBuilder sb = new StringBuilder();
      for(T t : list) {
         count ++;
         sb.append(t.toString());

         if(count != size) {
            sb.append(separator);
         }
      }
      str = sb.toString();
   }

   public String toString() {
      return str;
   }
}
