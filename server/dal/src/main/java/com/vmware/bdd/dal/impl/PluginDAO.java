package com.vmware.bdd.dal.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IPluginDAO;
import com.vmware.bdd.entity.PluginEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:55 PM
 */
@Repository
@Transactional(readOnly = true)
public class PluginDAO extends  BaseDAO<PluginEntity> implements IPluginDAO{
   @Override
   public PluginEntity findByName(String name) {
      return findUniqueByCriteria(Restrictions.eq("name", name));
   }
}
