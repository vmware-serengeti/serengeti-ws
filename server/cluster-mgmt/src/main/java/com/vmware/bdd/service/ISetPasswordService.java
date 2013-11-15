package com.vmware.bdd.service;

import java.util.ArrayList;

public interface ISetPasswordService {
   /**
    * Set password for nodes in cluster
    *
    * @param clusterName
    * @param ipsOfNodes
    * @param password
    * @return failed nodes list
    */
   public ArrayList<String> setPasswordForNodes(String clusterName, ArrayList<String> ipsOfNodes, String password);

   /**
    * Set password for node in cluster
    *
    * @param clusterName
    * @param fixedNodeIP
    * @param newPassword
    * @return success or not
    * @throws Exception
    */
   public boolean setPasswordForNode(String clusterName, String fixedNodeIP, String newPassword) throws Exception;
}