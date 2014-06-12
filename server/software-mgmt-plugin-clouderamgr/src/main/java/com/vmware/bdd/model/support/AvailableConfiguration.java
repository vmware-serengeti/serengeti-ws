package com.vmware.bdd.model.support;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 3:44 PM
 */
public class AvailableConfiguration {

   @Expose
   private String name;

   @Expose
   private boolean required;

   @Expose
   @SerializedName("default")
   private String defaultValue;

   @Expose
   private String displayName;

   @Expose
   private String description;

   @Expose
   private String relatedName;

   @Expose
   private String validationState; // TODO: remove since it maybe of no use

   @Expose
   private String validationMessage; // TODO: remove

}
