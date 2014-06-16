package com.vmware.bdd.dal;

import com.vmware.bdd.entity.PluginEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:53 PM
 */
public interface IPluginDAO {

   PluginEntity findByName(String name);

   void insert(PluginEntity pluginEntity);
}
