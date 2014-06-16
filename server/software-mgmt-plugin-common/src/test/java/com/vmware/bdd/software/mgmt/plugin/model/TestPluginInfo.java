package com.vmware.bdd.software.mgmt.plugin.model;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

public class TestPluginInfo {

   @Test
   public void testEquals() {
      Map<String, Object> cache = new HashMap<String, Object>();
      PluginInfo plugin1 = new PluginInfo();
      plugin1.setName("plugin1");
      plugin1.setHost("192.168.0.1");
      plugin1.setPort(1000);
      plugin1.setUsername("admin1");
      plugin1.setPassword("password1");
      plugin1.setPrivateKey("DA:93:D3:44:22");
      cache.put(plugin1.hashCode() + "", plugin1);
      PluginInfo plugin2 = new PluginInfo();
      plugin2.setName("plugin1");
      plugin2.setHost("192.168.0.1");
      plugin2.setPort(1000);
      plugin2.setUsername("admin1");
      plugin2.setPassword("password1");
      plugin2.setPrivateKey("DA:93:D3:44:22");
      assertNotNull(cache.get(plugin2.hashCode() + ""));
      PluginInfo p2 = (PluginInfo) cache.get(plugin2.hashCode() + "");
      assertEquals("plugin1", p2.getName());
      PluginInfo plugin3 = new PluginInfo();
      plugin3.setName("plugin1");
      plugin3.setHost("192.168.0.1");
      plugin3.setPort(1000);
      plugin3.setUsername("admin1");
      plugin3.setPassword("password1");
      plugin3.setPrivateKey("DA:93:D4:44:22");
      assertNull(cache.get(plugin3.hashCode() + ""));
   }
}
