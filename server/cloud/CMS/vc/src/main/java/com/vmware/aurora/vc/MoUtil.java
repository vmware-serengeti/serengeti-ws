/* ***************************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

package com.vmware.aurora.vc;

import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.TypeName;
import com.vmware.vim.vmomi.core.types.VmodlType;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;

/**
 * A utility class to support common <code>ManagedObject</code> and
 * <code>ManagedObjectReference</code> related tasks.
 */
public class MoUtil {
   private static final Logger logger = Logger.getLogger(MoUtil.class);

   private static final String MOREF_GUID_FORMAT = "%1s:%2s:%3s";

   /**
    * XXX This needs to be public only while we store moref values instead of
    * guids in the database. It should be deleted after migration to vc module.
    *
    * @param serverGuid
    * @param type
    * @param value
    * @return
    */
   public static String makeGuid(String serverGuid, String type, String value) {
      return String.format(
            MOREF_GUID_FORMAT,
            serverGuid,
            type,
            value);
   }
   /**
    * Returns a guid that represents the supplied ManagedObjectReference. Null
    * objects are represented by an empty string.
    */
   public static String morefToString(ManagedObjectReference moRef) {
      if (moRef == null) {
         return "";
      }
      return makeGuid(moRef.getServerGuid(),
            moRef.getType(),
            moRef.getValue());
   }

   /**
    * Reverse to the previous function: given a string in the format
    * MOREF_GUID_FORMAT, return ManagedObjectReference. Returns null if the
    * string has incorrect format.
    */
   public static ManagedObjectReference stringToMoref(String str) {
      String[] comps = str.split(":");

      if (comps.length != 2 && comps.length != 3) {
         return null;
      }
      // normalize null string values
      for (int i = 0; i < comps.length; i++) {
         if (comps[i] != null &&
             (comps[i].equals("null") || comps[i].equals(""))) {
            comps[i] = null;
         }
      }
      ManagedObjectReference ref = new ManagedObjectReference();
      if (comps.length == 2) {
         ref.setServerGuid(null);
         ref.setType(comps[0]);
         ref.setValue(comps[1]);
      } else {
         ref.setServerGuid(comps[0]);
         ref.setType(comps[1]);
         ref.setValue(comps[2]);
      }
      // make sure the reference object is valid
      if (ref.getType() == null || ref.getValue() == null ||
          (ref.getServerGuid() != null &&
           !ref.getServerGuid().equals(VcContext.getServerGuid()))) {
         return null;
      }
      return ref;
   }

   /**
    * Utility method to determine if the type of a specific
    * {@link ManagedObjectReference} instance is of the provided
    * <code>clazz</code>.
    *
    * The following example demonstrates the intended usage:
    * @code {
    *    boolean isStoragePod = ManagedObjectUtil.isOfType(entity, StoragePod.class);
    * }
    *
    * This method is more type safe and less error prone compared to the
    * version which accepts string parameter:
    * {@link MoUtil#isOfType(ManagedObjectReference, String)}
    *
    * @param entity
    *    The {@link ManagedObjectReference} instance which type to be
    *    compared.
    *
    * @param clazz
    *    The <code>Class</code> instance of the class for which we want to check.
    *
    * @return
    *    <code>true</code> if the <code>entity</code> is of the specified type or
    *    <code>false</code> otherwise.
    *
    * @see #isOfType(ManagedObjectReference, String)
    */
   public static boolean isOfType(ManagedObjectReference entity, Class<?> clazz) {
      if (entity == null || clazz == null) {
         return false;
      }
      VmodlType type = VmodlTypeMap.Factory.getTypeMap().getVmodlType(clazz);
      return type.getWsdlName().equalsIgnoreCase(entity.getType());
   }

   /**
    * Utility method to determine if the type of a specific refId
    * is of the provided <code>clazz</code>.
    * @param entityId: refId of the VIM object to check.
    * @param clazz: instance of the class we want to check for.
    * @return
    *    <code>true</code> if the object referred to by the
    *    <code>entityId</code> is of the specified type or
    *    <code>false</code> otherwise.
    */
   public static boolean isOfType(String entityId, Class<?> clazz) {
      return isOfType(stringToMoref(entityId), clazz);
   }

   /**
    * Convert a managed object reference to managed object.
    */
   public static <T extends ManagedObject> T
   getManagedObject(ManagedObjectReference moRef) {
      AuAssert.check(moRef != null &&
             moRef.getValue() != null &&
             (moRef.getServerGuid() == null ||
              moRef.getServerGuid().equals(VcContext.getServerGuid())));
      // Sun's javac requires explicit casting.
      // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
      AuAssert.check(VcContext.isInSession());
      return VcContext.getService().<T>getManagedObject(moRef);
   }

   /**
    * Convert a list of managed object reference to managed object list.
    */
   public static <T extends ManagedObject> List<T>
   getManagedObjects(ManagedObjectReference[] moRefs) throws Exception {
      ArrayList<T> list = new ArrayList<T>(moRefs.length);
      for (ManagedObjectReference moRef : moRefs) {
         list.add(MoUtil.<T>getManagedObject(moRef));
      }
      return list;
   }

   public static Folder getRootFolder() throws Exception {
      AuAssert.check(VcContext.isInSession());
      VcService svc = VcContext.getService();
      ManagedObjectReference rootFolderRef =
         svc.getServiceInstanceContent().getRootFolder();
      return svc.getManagedObject(rootFolderRef);
   }

   /**
    * Helper function to get all child entities of type <code>T</code>
    * in the <code>Folder</code> managed object.
    */
   public static <T extends ManagedObject> List<T>
   getChildEntity(Folder folder, Class<T> clazz)
   {
      AuAssert.check(VcContext.isInSession());
      VcService svc = VcContext.getService();
      ManagedObjectReference[] childEntities = folder.getChildEntity();
      List<T> entities = new ArrayList<T>();
      for (ManagedObjectReference child : childEntities) {
         if (isOfType(child, clazz)) {
            entities.add(svc.<T>getManagedObject(child));
         }
      }
      return entities;
   }

   /**
    * Similar to <code>getChildEntity()</code>, but does a deep traversal to
    * find all descendants of type <code>T</code> starting with a given folder.
    * @param <T>
    * @param folder
    * @param clazz
    * @return
    */
   public static <T extends ManagedObject> List<ManagedObjectReference>
   getDescendantsMoRef(Folder folder, Class<T> clazz) {
      List<ManagedObjectReference> descendants =
         new ArrayList<ManagedObjectReference>();
      getDescendants(folder, clazz, descendants);
      return descendants;
   }

   private static <T extends ManagedObject> void
   getDescendants(Folder folder, Class<T> clazz,
                  List<ManagedObjectReference> descendants)
   {
      AuAssert.check(VcContext.isInSession());
      VcService svc = VcContext.getService();
      ManagedObjectReference[] childEntities = folder.getChildEntity();
      for (ManagedObjectReference child : childEntities) {
         if (isOfType(child, clazz)) {
            descendants.add(child);
         } else if(isOfType(child, Folder.class)) {
            Folder childFolder = svc.<Folder>getManagedObject(child);
            getDescendants(childFolder, clazz, descendants);
         }
      }
   }

   /**
    * Traverse the managed entity tree to find an ancestor of type <T>.
    * @param <T>
    * @param parent
    * @param clazz
    * @return
    */
   protected static <T extends ManagedObject> ManagedObjectReference
   getAncestorMoRef(ManagedObjectReference parent, Class<T> clazz) {
      AuAssert.check(VcContext.isInSession());
      VcService svc = VcContext.getService();
      if (isOfType(parent, clazz)) {
         return parent;
      } else {
         ManagedObject obj = svc.getManagedObject(parent);
         if (obj instanceof Folder) {
            Folder folder = (Folder)obj;
            return getAncestorMoRef(folder.getParent(), clazz);
         }
      }
      logger.error("cannot find ancestor VC object of type " +
                   clazz.getName() + " for " + parent);
      throw AuroraException.INTERNAL();
   }

   /**
    * Returns TypeName for the passed moRef.
    * @param moRef
    * @return TypeName
    */
   public static TypeName getTypeName(ManagedObjectReference moRef) {
      return new TypeNameImpl(moRef.getType());
   }

   /**
    * Returns true when an exception is caused by connection or network
    * issues. This includes the connection problems due to vc being down.
    * @param e  exception to check
    * @return true for network related vc exceptions
    */
   public static boolean isNetworkException(Throwable e) {
      return e instanceof HttpHostConnectException || /* Temporary connectivity loss?      */
             e instanceof NoRouteToHostException   || /* Network glitch.                   */
             e instanceof SocketTimeoutException   || /* Client side. Session might be ok. */
             e instanceof SocketException;            /* Many cases including vc shutdown. */
   }

   /**
    * VLSI doesn't convert some special characters supported by VC
    * from URL format to String format on the receive path. See PR 737040.
    *
    * @param s
    * @return
    */
   public static String fromURLString(String s) {
      if (s == null) {
         return s;
      }
      StringBuffer buf = new StringBuffer();
      int i = 0;
      while (i < s.length()) {
         if (s.startsWith("%", i)) {
            // scan the next two characters
            String code = s.substring(i + 1, i + 3);
            if (code.equals("25")) {
               buf.append('%');
               i += 3;
               continue;
            }
            if (code.equals("2f") || code.equals("2F")){
               buf.append('/');
               i += 3;
               continue;
            }
            if (code.equals("5c") || code.equals("5C")){
               buf.append('\\');
               i += 3;
               continue;
            }
         }
         buf.append(s.charAt(i));
         i++;
      }
      return buf.toString();
   }

   public static String toURLString(String s) {
      if (s == null) {
         return s;
      }
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         switch (ch) {
         case '%':
            buf.append("%25");
            break;
         case '/':
            buf.append("%2f");
            break;
         case '\\':
            buf.append("%5c");
            break;
         default:
            buf.append(ch);
         }
      }
      return buf.toString();
   }
}
