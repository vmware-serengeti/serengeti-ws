package com.vmware.bdd.dal;

import com.vmware.bdd.entity.AppManagerEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:53 PM
 */
public interface IAppManagerDAO {

   AppManagerEntity findByName(String name);

   void insert(AppManagerEntity addManagerEntity);
}
