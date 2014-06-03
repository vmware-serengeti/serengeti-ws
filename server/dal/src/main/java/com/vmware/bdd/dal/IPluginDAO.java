package com.vmware.bdd.dal;

import com.vmware.bdd.apitypes.SoftwareMgtProvider;
import com.vmware.bdd.entity.PluginEntity;

import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:53 PM
 */
public interface IPluginDAO {

   PluginEntity findByName(String name);

   List<PluginEntity> findByProvider(SoftwareMgtProvider provider);

   void insert(PluginEntity pluginEntity);
}
