package com.vmware.bdd.apitypes;

import com.google.gson.annotations.Expose;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:52 PM
 */
public class CmConfigDef {

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   @Expose
   private  String value;
}
