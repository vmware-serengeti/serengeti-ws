package com.vmware.bdd.dal;

import java.util.List;

import com.vmware.bdd.entity.AppManagerEntity;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:53 PM
 */
public interface IAppManagerDAO {

   List<AppManagerEntity> findAll();

   List<AppManagerEntity> findAllSortByName();

   AppManagerEntity findByName(String name);

   void insert(AppManagerEntity addManagerEntity);
}
