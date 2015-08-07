package com.vmware.bdd.apitypes;

import java.util.Date;

public class VirtualMachineRead {

   private String moid;
   private String name;
   private Date lastModified;
   private String tag;

   public String getMoid() {
      return moid;
   }

   public void setMoid(String moid) {
      this.moid = moid;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Date getLastModified() {
      return lastModified;
   }

   public void setLastModified(Date lastModified) {
      this.lastModified = lastModified;
   }

   public String getTag() {
      return tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

}
