/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.dal;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;

import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.TxRetryException;
import com.vmware.bdd.exception.UniqueConstraintViolationException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Configuration;

/**
 * This static class serves as the database access layer for the rest
 * of the code.
 **/
public class DAL {
   private static final boolean stressTxnRollback = Configuration.getBoolean("dal.stressTxnRollback", false);
   private static final Logger logger = Logger.getLogger(DAL.class);
   private static final SessionFactory sessionFactory = buildSessionFactory();
   private static Random rnd = new Random();
   private static ThreadLocal<Boolean> inCheckedRegion = new ThreadLocal<Boolean>();
   private static final int defaultRetries = 5;

   private static void checkIsolationLevel (Session _ssn) throws Throwable {
      Transaction tx = null;

      try {
         tx = _ssn.beginTransaction();
         tx.commit();
      } catch (Throwable exc) {
         if (tx != null) {
            tx.rollback();
         }
         throw exc;
      }
   }

   private static SessionFactory buildSessionFactory() {
      try {
         SessionFactory _ssn = new org.hibernate.cfg.Configuration()
                                 .configure()
                                 .buildSessionFactory();
         checkIsolationLevel(_ssn.getCurrentSession());
         return _ssn;
      } catch (Throwable ex) {
         throw new ExceptionInInitializerError(ex);
      }
   }

   private static Session getSession() {
      return sessionFactory.getCurrentSession();
   }

   public static boolean isInTransaction() {
      return getSession().getTransaction().isActive();
   }

   /*
    * Logs a message when retrying a transaction.
    *
    * @param the "real" exception thrown from the transaction body. It can
    * be null if the body threw BddException with no cause.
    */
   private static void reportRetry(int retriesLeft, Throwable ex) {
      StackTraceElement[] bt = ex.getStackTrace();
      final int bodyFrame = 3; // After autoTransactionDo + findBy
      if (bt.length > bodyFrame) {
         // Print a notice but be concise about it.
         logger.info(ex + ". Retrying " + bt[bodyFrame] +
               ". " + retriesLeft + " attempts left.");
      } else {
         // Print the full backtrace, if it is short by itself.
         logger.info("Retrying. " + retriesLeft + " attempts left.", ex);
      }
   }

   /**
    * @param ex -- the "real" exception thrown from the transaction body. It can
    * be null if we throw BddException with no cause.
    * @return true if the given exception corresponds to Hibernate's
    * uniqueness violation.
    **/
   private static boolean isUniqViolation(Throwable ex) {
      final String uniquenessViolation = new String("23505"); // SQL State error
      return ex instanceof ConstraintViolationException &&
          ((ConstraintViolationException)ex).getSQLState().equals(uniquenessViolation);
   }

   /**
    * @param ex -- the "real" exception thrown from the transaction body. It can
    * be null if we throw BddException with no cause.
    * @return true if the given exception should cause a retry,
    * e.g. serialization failure or deadlock etc.
    **/
   private static boolean isRetryable(Throwable ex) {
      final String psqlDeadlockDetected = new String("40P01"); // SQL State error specific to PSQL
      return ((ex instanceof LockAcquisitionException)
              ||
              (ex instanceof GenericJDBCException &&
              ((GenericJDBCException)ex).getSQLState().equals(psqlDeadlockDetected)));
   }

   /**
    * Helper routine for wrapping a piece of code in a Hibernate transaction.
    *
    * @param obj -- the body of the transaction.
    * @param readOnly -- true if the writes are to be disallowed
    * @param retriesLeft -- the max number of times to retry on lock-acquisition exceptions.
    * 0 if retries are to be disallowed.
    **/
   @SuppressWarnings("deprecation")
   private static <T> T inTransactionDoWork(Saveable<T> obj, boolean readOnly, int retriesLeft) {
      T retval;
      while (true) {
         Session sn = getSession();
         Transaction tx = null;
         FlushMode flushMode = null;
         boolean doRndRollback = ConfigInfo.isDebugEnabled() &&
                                  stressTxnRollback && (rnd.nextInt() % 5) == 0;
         AuAssert.check(!isInTransaction()); // Disallow nesting for now.
         try {
            tx = sn.beginTransaction();

            if (readOnly && tx != null) {
               flushMode = sn.getFlushMode();
               sn.setFlushMode(FlushMode.MANUAL);
            }

            sn.connection().setReadOnly(readOnly);
            retval = obj.body();
            if (doRndRollback) {
               logger.debug("randomly rollback the transaction");
               throw new LockAcquisitionException("Random Rollback",
                        new SQLException("Random Rollback"));
            }

            if (flushMode != null) {
               sn.setFlushMode(flushMode);
            }

            tx.commit();
            break; // must come right after commit
         } catch (Throwable ex) {
            if (tx != null) {
               if (flushMode != null) {
                  sn.setFlushMode(flushMode);
               }
               tx.rollback();
               flushTransactionCallbacks(false);
            }
            // Strip off the BddException wrapper if a callee added it.
            Throwable realEx = (ex instanceof BddException) ? ex.getCause() : ex;
            if (isRetryable(realEx)) {
               if (retriesLeft > 0) {
                  if (!doRndRollback) {
                     retriesLeft--;
                     reportRetry(retriesLeft, realEx);
                  }
               } else {
                  throw TxRetryException.wrap(realEx, doRndRollback);
               }
            } else if (isUniqViolation(realEx)) {
               throw UniqueConstraintViolationException.wrap((ConstraintViolationException)realEx);
            } else {
               throw BddException.wrapIfNeeded(ex, "Exception in a DAL transaction");
            }
         }
      }
      flushTransactionCallbacks(true);
      return retval;
   }

   /**
    * Enters a checked region: either a read-write transaction or a blocking
    * operation (only VC operations, at the moment). That is, we do not allow
    * read-write transactions in VC sessions, and we do not allow VC sessions
    * in read-write transactions. The goal is to avoid conflicts between
    * two long-running read-write transactions.
    **/
   private static void enterCheckedRegion() {
      Boolean oldVal = inCheckedRegion.get();
      AuAssert.check(oldVal == null || !oldVal);
      inCheckedRegion.set(true);
   }

   /**
    * Leaves a checked region.
    **/
   private static void leaveCheckedRegion() {
      AuAssert.check(inCheckedRegion.get());
      inCheckedRegion.set(false);
   }

   /**
    * Helper routine for wrapping a piece of code in a Hibernate transaction.
    *
    * @param obj -- the body of the transaction. It may be executed multiple
    * times if a retryable exception is encountered.
    **/
   public static <T> T inTransactionDo(Saveable<T> obj) {
      return inTransactionDoWork(obj, false, defaultRetries);
   }

   /**
    * Similar to inTransactionDo, but allows only read-only database operations
    * in this transaction. Attempt to write something from such a transaction
    * will result in a jdbc exception. We do allow read-only transactions in
    * VC sessions and vice-versa.
    **/
   public static <T> T inRoTransactionDo(Saveable<T> obj) {
      return inTransactionDoWork(obj, true, defaultRetries);
   }

   /**
    * Similar to inTransactionDo, but runs the transaction in a checked region.
    * TODO: enter/leave the checked region directly from inTransactionDo and
    * get rid of this routine. At the moment however, we have many cases of VC
    * operations in RW transactions, so it would break. Until that is fixed,
    * we use explicit inRwTransactionDo for known-good cases, like all annotated
    * UI and WSDL requests.
    **/
   public static <T> T inRwTransactionDo(Saveable<T> obj) {
      enterCheckedRegion();
      try {
         return inTransactionDoWork(obj, false, defaultRetries);
      } finally {
         leaveCheckedRegion();
      }
   }

   /**
    * Helper routine for wrapping a piece of code in a Hibernate transaction if no transaction is active.
    * It doesn't start a new transaction if it's already in one.
    *
    * @param obj -- the body of the transaction.
    **/
   public static <T> T autoTransactionDo(Saveable<T> obj) {
      if (isInTransaction()) {
         try {
            return obj.body();
         } catch (Exception exc) {
            throw BddException.wrapIfNeeded(exc, "Exception in a DAL transaction");
         }
      } else {
         return inTransactionDo(obj);
      }
   }

   /**
    * Fetch and return an object of the given class and id from the database.
    *
    * @param aClass -- the class of the object.
    * @param id -- the id of the object.
    * @return The reference to the object or null if the object is not found.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static <T> T findById(final Class<T> aClass, final Serializable id) {
      return autoTransactionDo(new Saveable<T>() {
         @SuppressWarnings("unchecked")
         public T body() {
            return (T) getSession().get(aClass, id);
         }
      });
   }

   /**
    * Fetch and return a list of objects of the given class that
    * satisfy the given criteria.
    *
    * @param aClass -- the class of the object.
    * @param queryCriteria -- the criteria to filter the results.
    * @return The list of matching objects.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static <T> List<T> findByCriteria(final Class<T> aClass,
         final Criterion... queryCriteria) {
      return findByCriteria(aClass, null, null, null, queryCriteria);
   }


   /**
    * Fetch and return an object that satisfies the given criteria.
    *
    * @param aClass -- the class of the object.
    * @param queryCriteria -- the criteria to filter the results.
    * @return The matching object or null if the object is not found.
    * If the object is not unique, an assertion is thrown.
    **/
   public static <T> T findUniqueByCriteria(Class<T> aClass,
                                             Criterion... queryCriteria) {
      List<T> results = findByCriteria(aClass, queryCriteria);
      if (results.size() == 0) {
         return null;
      } else {
         AuAssert.check(results.size() == 1);
         return results.get(0);
      }
   }

   /**
    * Fetch and return a list of objects of the given class which
    * is a subset of the all the records that satisfy the given
    * criteria. The subset is specified by parameter firstResult
    * and maxResults.
    *
    * @param aClass -- the class of the object.
    * @param order -- the result set order.
    * @param firstResult -- the offset in the query result where the returned list starts.
    * @param maxResults -- the maximum records returned.
    * @param queryCriteria -- the criteria to filter the results.
    * @return The list of matching objects.
    * @throws HibernateException -- on Hibernate errors.
    */
   private static <T> List<T> findByCriteria(final Class<T> aClass,
            final Order[] order, final Integer firstResult, final Integer maxResults,
            final Criterion... queryCriteria) {
      return autoTransactionDo(new Saveable<List<T>>() {
         @SuppressWarnings("unchecked")
         public List<T> body() {
            Criteria criteria = getSession().createCriteria(aClass);
            for (Criterion c : queryCriteria) {
               criteria.add(c);
            }

            if (order != null) {
               for (Order o:order) {
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
      });
   }

   /**
    * Fetch and return a list of all objects of the given class from
    * the database.
    *
    * @param aClass -- the class of the object.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static <T> List<T> findAll(Class<T> aClass) {
      return findByCriteria(aClass);
   }

   /**
    * Fetch and return a list of objects of the given class from
    * the database in the requested <code>order</code>.
    *
    * @param aClass -- the class of the object.
    * @param order -- the result set order.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static <T> List<T> findAll(Class<T> aClass, Order[] order) {
      return findByCriteria(aClass, order, null, null);
   }

   /**
    * Save a newly-created object to the database.
    *
    * @param obj -- the object to save.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static void insert(Object obj) {
      getSession().save(obj);
   }

   /**
    * Update a persistent object in the database.
    *
    * @param obj -- the object to save.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static void update(Object obj) {
      getSession().update(obj);
   }


   /**
    * Delete the given object from the database.
    *
    * @param obj -- the object to delete.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static void delete(Object obj) {
      getSession().delete(obj);
   }

   /**
    * Refresh/reload the given object from the database.
    *
    * @param obj -- the object to refresh.
    * @throws HibernateException -- on Hibernate errors.
    **/
   public static void refresh(Object obj) {
      getSession().refresh(obj);
   }

   // Convenience routines for wrapping <code>findById, findAll,
   // findByCriteria, save, update, delete</code> in a transaction.
   public static void inTransactionInsert(final Object obj) {
      inTransactionDo(new Saveable<Void>() {
         public Void body() {
            insert(obj);
            return null;
         }
      });
   }

   public static void inTransactionUpdate(final Object obj) {
      inTransactionDo(new Saveable<Void>() {
         public Void body() {
            update(obj);
            return null;
         }
      });
   }

   public static void inTransactionRefresh(final Object obj) {
      inTransactionDo(new Saveable<Void>() {
         public Void body() {
            refresh(obj);
            return null;
         }
      });
   }

   public static void inTransactionDelete(final Object obj) {
      inTransactionDo(new Saveable<Void>() {
         public Void body() {
            delete(obj);
            return null;
         }
      });
   }

   /*
    * API for transaction-complete callbacks
    */
   private interface Callback {
      void onTransactionComplete(boolean committed);
   }

   private static ThreadLocal<ArrayList<Callback>> txCompleteCallbacks = new ThreadLocal<ArrayList<Callback>>();

   // no synchronization needed, as txCompleteCallbacks is thread local
   // we don't have to care which transaction, until/unless nested transactions are allowed
   private static void flushTransactionCallbacks(boolean committed) {
      AuAssert.check(!isInTransaction());
      ArrayList<Callback> cbs = txCompleteCallbacks.get();
      if (cbs != null) {
         txCompleteCallbacks.set(null);
         for (Callback cb: cbs) {
            cb.onTransactionComplete(committed);
         }
      }
   }
}