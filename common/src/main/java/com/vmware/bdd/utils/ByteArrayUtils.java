/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.utils;

/**
 * Created By xiaoliangl on 12/11/14.
 */
public class ByteArrayUtils {

   final private static char[] hexRepresentation = "0123456789ABCDEF".toCharArray();

   /**
    * Convert a byte array to hexadecimal
    * @param array an array of bytes
    * @return hex String representation of the input
    */
   public static char[] byteArrayToHexChars(final byte[] array) {
      char[] hexChars = new char[array.length * 2];
      int b;
      for(int i=0; i < array.length; i++) {
         b = array[i] & 0xFF;
         hexChars[2*i + 0] = hexRepresentation[b >>> 4];
         hexChars[2*i + 1] = hexRepresentation[b & 0x0F];
      }
      return hexChars;
   }

   /**
    * Convert a byte array to hexadecimal
    * @param array an array of bytes
    * @return hex String representation of the input
    */
   public static String byteArrayToHexString(final byte[] array) {
      return new String(byteArrayToHexChars(array));
   }
}
