package com.vmware.bdd.model.support;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 3:37 PM
 */
public class AvailableServiceRole {

   public static final int VERSION_UNBOUNDED = -1;

   @Expose
   private String name;

   @Expose
   private AvailableServiceRole parent;

   @Expose
   private AvailableParcelRepo repository;

   @Expose
   private int versionApiMin;

   @Expose
   private int versionApiMax;

   @Expose
   private int versionCdhMin;

   @Expose
   private int versionCdhMax;

   @Expose
   private List<AvailableConfiguration> availableConfigurations;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public AvailableServiceRole getParent() {
      return parent;
   }

   public void setParent(AvailableServiceRole parent) {
      this.parent = parent;
   }

   public AvailableParcelRepo getRepository() {
      return repository;
   }

   public void setRepository(String repository) {
      this.repository = AvailableParcelRepo.valueOf(repository);
   }

   public int getVersionApiMin() {
      return versionApiMin;
   }

   public void setVersionApiMin(int versionApiMin) {
      this.versionApiMin = versionApiMin;
   }

   public int getVersionApiMax() {
      return versionApiMax;
   }

   public void setVersionApiMax(int versionApiMax) {
      this.versionApiMax = versionApiMax;
   }

   public int getVersionCdhMin() {
      return versionCdhMin;
   }

   public void setVersionCdhMin(int versionCdhMin) {
      this.versionCdhMin = versionCdhMin;
   }

   public int getVersionCdhMax() {
      return versionCdhMax;
   }

   public void setVersionCdhMax(int versionCdhMax) {
      this.versionCdhMax = versionCdhMax;
   }

   public List<AvailableConfiguration> getAvailableConfigurations() {
      return availableConfigurations;
   }

   public void setAvailableConfigurations(List<AvailableConfiguration> availableConfigurations) {
      this.availableConfigurations = availableConfigurations;
   }

}
