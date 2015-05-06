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
package com.vmware.bdd.usermgmt;

import java.lang.reflect.Field;

public class TestUtils {

    public static boolean setPrivateField(final Object obj, final String name, final Object value) {
        if (null == obj)
            throw new NullPointerException("obj can't be null!");
        if (null == name)
            throw new NullPointerException("name can't be null!");
        boolean result = true;
        Class<?> clazz = obj.getClass();

        Field field = null;
        try {
            field = clazz.getDeclaredField(name);

            field.setAccessible(true);
            field.set(obj, value);
        } catch (SecurityException e) {
            result = false;
        } catch (NoSuchFieldException e) {
            result = false;
        } catch (IllegalArgumentException e) {
            result = false;
        } catch (IllegalAccessException e) {
            result = false;
        }
        return result;
    }
}
