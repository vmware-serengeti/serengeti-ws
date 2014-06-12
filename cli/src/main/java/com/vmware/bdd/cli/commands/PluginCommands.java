package com.vmware.bdd.cli.commands;

import com.vmware.bdd.apitypes.PluginAdd;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.PluginRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:18 PM
 */
@Component
public class PluginCommands implements CommandMarker {

   @Autowired
   private PluginRestClient pluginRestClient;

   @CliAvailabilityIndicator({ "plugin help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "plugin add", help = "add a vendor instance")
   public void addPlugin(
         @CliOption(key = { "name" }, mandatory = true, help = "The instance name") final String name,
         @CliOption(key = { "provider" }, mandatory = true, help = "The provider type, ClouderaManager or Ambari") final String provider,
         @CliOption(key = { "host" }, mandatory = true, help = "The host address") final String host,
         @CliOption(key = { "port" }, mandatory = false, unspecifiedDefaultValue = "-1", help = "The port") final int port,
         @CliOption(key = { "username" }, mandatory = false, help = "The username to login software manager server") final String username,
         @CliOption(key = { "password"}, mandatory = false, help = "The password") final String password,
         @CliOption(key = { "privateKeyFile"}, mandatory = true, help = "The private key file for communication between server and agents") final String path) {

      // rest invocation
      try {
         PluginAdd pluginAdd = new PluginAdd();
         pluginAdd.setName(name);
         pluginAdd.setProvider(provider);
         pluginAdd.setHost(host);
         pluginAdd.setPort(port);
         pluginAdd.setUsername(username);
         pluginAdd.setPassword(password);
         pluginAdd.setPrivateKey(CommandsUtils.dataFromFile(path));
         pluginRestClient.add(pluginAdd);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_PLUGIN, name,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }
}
