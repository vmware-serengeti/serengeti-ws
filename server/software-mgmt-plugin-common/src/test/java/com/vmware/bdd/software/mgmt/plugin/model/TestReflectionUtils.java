package com.vmware.bdd.software.mgmt.plugin.model;

import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 7/29/14
 * Time: 11:55 AM
 */
public class TestReflectionUtils {

   @Test
   public void testGetClass() {
      Class<? extends Object> clazz = ReflectionUtils.getClass(
            "com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint", Object.class);
      ReflectionUtils.newInstance(clazz);
   }
}
