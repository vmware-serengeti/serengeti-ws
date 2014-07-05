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
package com.vmware.bdd.plugin.ambari.api.utils;

import java.lang.reflect.Constructor;
import java.util.Date;

import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.vmware.bdd.plugin.ambari.api.Parameters;

public final class ApiUtils {
   private static final Logger logger = Logger.getLogger(ApiUtils.class);

   private static final DateTimeFormatter DATE_TIME_PRINTER = ISODateTimeFormat.dateTime();
   private static final DateTimeFormatter DATE_TIME_PARSER = ISODateTimeFormat.dateTimeParser();
   private static final PeriodFormatter PERIOD_FORMATTER = ISOPeriodFormat.standard();


   /**
    * A special path component contained in the path for an HDFS snapshot dir.
    *
    * Copied from org.apache.hadoop.hdfs.protocol.HdfsConstants.
    */
   public final static String DOT_SNAPSHOT_DIR = ".snapshot";

   public static Instant newInstantFromString(String value) {
     if (value.equalsIgnoreCase(Parameters.DATE_TIME_NOW)) {
       return new Instant();
     }

     return new Instant(DATE_TIME_PARSER.parseMillis(value));
   }

   public static Date newDateFromString(String value) {
     return new Date(newInstantFromString(value).getMillis());
   }

   public static Date newDateFromMillis(long millis) {
     return new Date(millis);
   }

   public static Period newPeriodFromString(String value) {
     return PERIOD_FORMATTER.parsePeriod(value);
   }

   public static String printDate(Date date) {
     return DATE_TIME_PRINTER.print(new Instant(date));
   }

   public static String printDate(Instant instant) {
     return DATE_TIME_PRINTER.print(instant);
   }

   /**
    * Calculate the fromDate.
    * If the fromString is not provided, then the fromDate calculated
    * from the toDate and the window.
    * @param fromString A string representation of the from date.
    * @param toDate The to date for this period
    * @param window The duration of this period
    * @return the Date object that corresponds to the from date
    */
   public static Date getFromDate(
       String fromString, Date toDate, Duration window) {
     Date fromDate = null;
     if (fromString != null) {
       fromDate = newDateFromString(fromString);
       Preconditions.checkArgument(
           fromDate.getTime() < toDate.getTime(),
           "Invalid period specified: 'to' must be later than 'from'.");
     } else {
       Instant fromInstant = new Instant(toDate.getTime()).minus(window);
       fromDate = new Date(fromInstant.getMillis());
     }
     return fromDate;
   }

   /**
    * Checks whether the <i>other</i> object has the same class as the given
    * object, and casts it to the given object's type.
    * <p>
    * The pattern for using this in an implementation of equals() would be:
    * <blockquote><tt>
    *   MyType that = ApiUtils.baseEquals(this, other);
    *   return this == that || (that != null &&
    *       Objects.equal(...) &&
    *       Objects.equal(...));
    * </tt></blockquote>
    *
    * @param object The object being compared.
    * @param other The object being compared against.
    * @return <i>other</i>, if it's not null and has the same class as
    *         <i>object</i>; null otherwise.
    */
   public static <T> T baseEquals(T object, Object other) {
     if (object == other ||
         (other != null && object.getClass() == other.getClass())) {
       return (T) other;
     }
     return null;
   }

   /**
    * Check that the given values are sane.
    *
    * @param offset Value to use as offset of a list.
    * @param limit Value to use as limit of a list's size.
    */
   public static void checkOffsetAndLimit(int offset, int limit) {
     Preconditions.checkArgument(offset >= 0,
         "Offset should be greater or equal 0.");
     Preconditions.checkArgument(limit > 0,
         "Limit should be greater than 0.");
   }
   
   public static <T> T jsonToObject(Class<T> entityClass, String apiJson) {
      T entity = null;
      try {
         Constructor<T> meth = (Constructor<T>) entityClass.getConstructor();
         entity = meth.newInstance();
         Gson gson = new Gson();
         entity = gson.fromJson(apiJson, entityClass);
      } catch (Exception e) {
         logger.error(e.getMessage());
      }
      return entity;
   }

   public static String objectToJson(Object object) {
      String jsonString = null;
      try {
         Gson gson = new Gson();
         jsonString = gson.toJson(object);
      } catch (Exception e) {
         logger.error(e.getMessage());
      }
      return jsonString;
   }

   private ApiUtils() { }

}
