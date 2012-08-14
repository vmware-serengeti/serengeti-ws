package com.vmware.bdd.frontend;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;

public class RestClient {

    protected RestTemplate restTemplate;
    private String serengetiUrl;
    
    public RestClient(String serverIp) {
        serengetiUrl = "http://" + serverIp + ":8080/serengeti/api";
        
        restTemplate = new RestTemplate();
    }

    public void deleteCluster(String name) {
        restTemplate.delete(serengetiUrl + "/cluster/{name}", name);
    }
   
    public String createCluster(ClusterCreate cluster) {
/*        HttpEntity<String> request;
        HttpHeaders headers = new HttpHeaders();
        
        String json = "{\"name\": \"" + cluster.getName() + "\"}";

        headers.setContentType(MediaType.APPLICATION_JSON);
        request = new HttpEntity<String>(json, headers);
  */      
        return restTemplate.postForObject(serengetiUrl + "/clusters", cluster, String.class);
    }
    
    public List<ClusterRead> getClusters() {
        ClusterRead[] clusterArray = restTemplate.getForObject(serengetiUrl + "/clusters", 
                ClusterRead[].class);
        
        return Arrays.asList(clusterArray);
    }
    
    public List<ResourcePoolRead> getResourcePools() {
        ResourcePoolRead[] rpArray = restTemplate.getForObject(serengetiUrl + "/resourcepools", 
                ResourcePoolRead[].class);
        
        return Arrays.asList(rpArray);
    }
    
    public List<DatastoreRead> getDatastores() {
        DatastoreRead[] dsArray = restTemplate.getForObject(serengetiUrl + "/datastores", 
                DatastoreRead[].class);
        
        return Arrays.asList(dsArray);
    }
    
    public List<NetworkRead> getNetworks() {
        NetworkRead[] networkArray = restTemplate.getForObject(serengetiUrl + "/networks", 
                NetworkRead[].class);
        
        return Arrays.asList(networkArray);
    }    
    
    public ClusterRead getClusterByName(String name) {
        ClusterRead cluster = restTemplate.getForObject(serengetiUrl + "/clusters/{name}", 
                ClusterRead.class);
        
        return cluster;
    }
    
}
