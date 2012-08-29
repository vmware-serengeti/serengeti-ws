package com.vmware.bdd.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.frontend.entities.ConnectForm;

@Controller
public class FrontendController {
	
    private String serengetiServer;
	private RestClient client = new RestClient();
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model) {

		if (serengetiServer == null || "".equals(serengetiServer)) {
            return "connect";
	    }
            
	    model.addAttribute("serengetiServer", serengetiServer);

	    return "home";
	}
	
	//
	// Connect/Disconnect
	//

	@ModelAttribute("ConnectForm")
    public ConnectForm connectForm() {
        return new ConnectForm();
    }
	
    @RequestMapping(value = "/action/connect", method = RequestMethod.POST)
    public String connect(@ModelAttribute ConnectForm form) {
        serengetiServer = form.getSerengetiServer();

        client.connect(serengetiServer, form.getUsername(), form.getPassword());
        
        return "redirect:/";
    }

    @RequestMapping(value = "/action/disconnect", method = RequestMethod.GET)
    public String disconnect() {
        serengetiServer = "";
        
        client.disconnect();
        
        return "redirect:/";
    }
    
	//
	// UI Views 
	//

    @RequestMapping(value = "/clusters", method = RequestMethod.GET)
    public String getClusters(Model model) {
        
        model.addAttribute("clusterList", client.getClusters());
        return "clusters";
    }
	
    @RequestMapping(value = "/resources", method = RequestMethod.GET)
    public String getResources(Model model) {
        
        model.addAttribute("resourcePools", client.getResourcePools());
        model.addAttribute("datastores", client.getDatastores());
        model.addAttribute("networks", client.getNetworks());
        return "resources";
    }
    
    //
    // Actions
    //
    
	@RequestMapping(value = "/action/cluster/create", method = RequestMethod.POST)
	public String createCluster(@ModelAttribute ClusterCreate cluster) {
		logger.info("Create Cluster");
	    client.createCluster(cluster);
	    return "redirect:/";
	}

    @RequestMapping(value = "/action/cluster/delete/{name}", method = RequestMethod.GET)
    public String deleteCluster(@PathVariable String name) {
        client.deleteCluster(name);
        return "redirect:/";
    }
	
}
