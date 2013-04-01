/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.dal.impl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IBaseDAO;
import com.vmware.bdd.utils.AuAssert;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public abstract class BaseDAO<T> implements IBaseDAO<T> {

   private static final Logger logger = Logger.getLogger(BaseDAO.class);

   protected SessionFactory sessionFactory;

   private final Class<T> type;

   @SuppressWarnings("unchecked")
   public BaseDAO() {
      this.type =
            (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                  .getActualTypeArguments()[0];
   }

   /**
    * @return the sessionFactory
    */
   public SessionFactory getSessionFactory() {
      return sessionFactory;
   }

   /**
    * @param sessionFactory
    *           the sessionFactory to set
    */
   @Autowired
   public void setSessionFactory(SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.dal.IBaseDAO#insert(T)
    */
   @Override
   @Transactional
   public void insert(T object) {
      this.getSessionFactory().getCurrentSession().save(object);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.dal.IBaseDAO#delete(T)
    */
   @Override
   @Transactional
   public void delete(T object) {
      this.sessionFactory.getCurrentSession().delete(object);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.dal.IBaseDAO#update(T)
    */
   @Override
   @Transactional
   public void update(T object) {
      this.sessionFactory.getCurrentSession().update(object);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.dal.IBaseDAO#findAll()
    */
   @Override
   @SuppressWarnings("unchecked")
   @Transactional(readOnly = true)
   public List<T> findAll() {
      logger.debug("findAll");
      List<T> result = null;
      Criteria query =
            this.sessionFactory.getCurrentSession().createCriteria(type);
      result = query.list();
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.dal.IBaseDAO#findById(java.io.Serializable)
    */
   @Override
   @SuppressWarnings("unchecked")
   @Transactional(readOnly = true)
   public T findById(Serializable id) {
      return (T) this.sessionFactory.getCurrentSession().get(type, id);
   }

   /**
    * Fetch and return a list of objects of the given class that satisfy the
    * given criteria.
    *
    * @param queryCriteria
    *           -- the criteria to filter the results.
    * @return The list of matching objects.
    * @throws HibernateException
    *            -- on Hibernate errors.
    **/
   @Override
   @Transactional(readOnly = true)
   public List<T> findByCriteria(final Criterion... queryCriteria) {
      return findByCriteria(null, null, null, queryCriteria);
   }


   /**
    * Fetch and return an object that satisfies the given criteria.
    *
    * @param queryCriteria
    *           -- the criteria to filter the results.
    * @return The matching object or null if the object is not found. If the
    *         object is not unique, an assertion is thrown.
    **/
   @Override
   @Transactional(readOnly = true)
   public T findUniqueByCriteria(Criterion... queryCriteria) {
      List<T> results = findByCriteria(queryCriteria);
      if (results.size() == 0) {
         return null;
      } else {
         AuAssert.check(results.size() == 1);
         return results.get(0);
      }
   }

   /**
    * Fetch and return a list of objects of the given class which is a subset of
    * the all the records that satisfy the given criteria. The subset is
    * specified by parameter firstResult and maxResults.
    *
    * @param order
    *           -- the result set order.
    * @param firstResult
    *           -- the offset in the query result where the returned list
    *           starts.
    * @param maxResults
    *           -- the maximum records returned.
    * @param queryCriteria
    *           -- the criteria to filter the results.
    * @return The list of matching objects.
    * @throws HibernateException
    *            -- on Hibernate errors.
    */
   @SuppressWarnings("unchecked")
   protected List<T> findByCriteria(final Order[] order,
         final Integer firstResult, final Integer maxResults,
         final Criterion... queryCriteria) {
      Criteria criteria =
            sessionFactory.getCurrentSession().createCriteria(type);
      for (Criterion c : queryCriteria) {
         criteria.add(c);
      }

      if (order != null) {
         for (Order o : order) {
            criteria.addOrder(o);
         }
      }

      if (firstResult != null) {
         criteria.setFirstResult(firstResult);
      }

      if (maxResults != null) {
         criteria.setMaxResults(maxResults);
         criteria.setFetchSize(maxResults); // just a hint
      }

      return criteria.list();
   }
}
