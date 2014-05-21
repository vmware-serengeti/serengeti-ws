package com.vmware.bdd.apitypes;

/**
 * Author: Xiaoding Bian
 * Date: 5/22/14
 * Time: 3:20 PM
 */
public class CmServiceSpec implements Comparable<CmServiceSpec> {

   public enum CmServiceStatus {
      STARTING, STARTED, STOPPING, STOPPED, BUSY, UNKNOWN
   }

   public static final int VERSION_UNBOUNDED = -1;
   public static final String NAME_TOKEN_DELIM = "_";
   public static final String NAME_TAG_DEFAULT = "cdh";
   public static final String NAME_QUALIFIER_DEFAULT = "1";
   public static final String NAME_QUALIFIER_GROUP = "group";

   private String name;
   private String group;
   private CmServiceType type;
   private String tag;
   private String qualifier;
   private String host;
   private String ip;
   private String ipInternal;
   private String toString;

   private transient CmServiceStatus status = CmServiceStatus.UNKNOWN;

   @Override
   public int compareTo(CmServiceSpec o) {
      return 0;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public boolean equals(Object object) {
      if (object instanceof CmServiceSpec) {
         return toString().equals(object.toString());
      }
      return false;
   }

   @Override
   public String toString() {
      // toString can be cached given object is immutable
      if (toString == null) {
         StringBuilder string = new StringBuilder();
         string.append("{");
         string.append("name=");
         string.append(name);
         string.append(", ");
         string.append("group=");
         string.append(group);
         string.append(", ");
         string.append("type=");
         string.append(type);
         string.append(", ");
         string.append("tag=");
         string.append(tag);
         string.append(", ");
         string.append("qualifier=");
         string.append(qualifier);
         string.append(", ");
         string.append("host=");
         string.append(host);
         string.append(", ");
         string.append("ip=");
         string.append(ip);
         string.append(", ");
         string.append("ipInternal=");
         string.append(ipInternal);
         string.append("}");
         toString = string.toString();
      }
      return toString;
   }

     public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
  }

  public CmServiceType getType() {
    return type;
  }

  public String getTag() {
    return tag;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getHost() {
    return host;
  }

  public String getIp() {
    return ip;
  }

  public String getIpInternal() {
    return ipInternal;
  }

  public CmServiceStatus getStatus() {
    return status;
  }

}
