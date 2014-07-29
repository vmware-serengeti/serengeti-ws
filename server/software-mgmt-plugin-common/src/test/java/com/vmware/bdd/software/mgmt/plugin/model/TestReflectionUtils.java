package com.vmware.bdd.software.mgmt.plugin.model;

import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 7/29/14
 * Time: 11:55 AM
 */
public class TestReflectionUtils {

   @Test
   public void testGetClass() throws IllegalAccessException, InstantiationException {
      Class<? extends Object> clazz = ReflectionUtils.getClass("com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint", Object.class);
      ClusterBlueprint blueprint = (ClusterBlueprint)clazz.newInstance();
   }
}
