package com.vmware.bdd.model.support;

import com.google.common.collect.ImmutableMap;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 3:32 PM
 */

public enum AvailableParcelRepo {

  CDH, IMPALA, SOLR, SPARK;

  private static final ImmutableMap<String, ImmutableMap<String, String>> TYPE_TO_REPO = ImmutableMap.of("CDH4",
      ImmutableMap.of("CDH", "CDH", "IMPALA", "IMPALA", "SOLR", "SOLR", "SPARK", "SPARK"), "CDH5",
      ImmutableMap.of("CDH", "CDH", "IMPALA", "CDH", "SOLR", "CDH", "SPARK", "CDH"));

  public String toString(String cdh) {
    if (!TYPE_TO_REPO.containsKey(cdh) || !TYPE_TO_REPO.get(cdh).containsKey(this.toString())) {
      throw new RuntimeException("Could not determine repo for [" + cdh + "] and [" + this + "]");
    }
    return TYPE_TO_REPO.containsKey(cdh) ? TYPE_TO_REPO.get(cdh).get(this.toString()) : null;
  }

};