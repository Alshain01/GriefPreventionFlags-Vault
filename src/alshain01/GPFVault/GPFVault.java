/* Copyright 2013 Kevin Seiden. All rights reserved.

 This works is licensed under the Creative Commons Attribution-NonCommercial 3.0

 You are Free to:
    to Share — to copy, distribute and transmit the work
    to Remix — to adapt the work

 Under the following conditions:
    Attribution — You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
    Non-commercial — You may not use this work for commercial purposes.

 With the understanding that:
    Waiver — Any of the above conditions can be waived if you get permission from the copyright holder.
    Public Domain — Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
    Other Rights — In no way are any of the following rights affected by the license:
        Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
        The author's moral rights;
        Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.

 Notice — For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 http://creativecommons.org/licenses/by-nc/3.0/
*/

package src.alshain01.GPFVault;

import java.io.File;
import java.io.IOException;

import me.ryanhamshire.GriefPrevention.Claim;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import src.john01dav.GriefPreventionFlags.ClaimManager;
import src.john01dav.GriefPreventionFlags.Flag;
import src.john01dav.GriefPreventionFlags.Flag.Type;
import src.john01dav.GriefPreventionFlags.Messages;

/**
 * GriefPreventionFlags - Vault
 * 
 * @author Kevin Seiden
 */
public class GPFVault extends JavaPlugin {
	public static GPFVault instance;
	protected Economy economy = null;
	protected CustomConfig messages = new CustomConfig("messages.yml");
	private final boolean DEBUG = false;

	/**
	 * Called when this plug-in is enabled 
	 */
	@Override
	public void onEnable() {
		instance = this;
		// Create the configuration file if it doesn't exist
		if(!(new File(this.getDataFolder() + "config.yml").exists())) {
			this.saveDefaultConfig();

			for (Type t : Type.values()) {
				this.getConfig().set("Price." + EPurchaseType.Flag.toString() + "." + t.toString(), "0");
				this.getConfig().set("Price." + EPurchaseType.Message.toString() + "." + t.toString(), "0");
			}
			this.saveConfig();
		}
		
		// Create the messages file if it doesn't exist
		if(!(new File(this.getDataFolder() + "messages.yml").exists())) {
			messages.saveDefaultConfig();
		}
		
		// Register for listeners
		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new GPFlagsListener(), instance);
		
		// Check for GPFlags
		if(!manager.isPluginEnabled("GriefPrevention")){
			getLogger().severe("<Dependency Error> GriefPrevention was not found!");
			manager.disablePlugin(this);
            return;
		}
		
		// Check for GPFlags
		if(!manager.isPluginEnabled("GriefPreventionFlags")){
			getLogger().severe("<Dependency Error> GriefPreventionFlags was not found!");
			manager.disablePlugin(this);
            return;
		}
		
		// Check for Vault
        if (!setupEconomy() ) {
            this.getLogger().severe("<Dependency Error> Vault was not found!");
            manager.disablePlugin(this);
            return;
        }
        
        // Enable MetricsLite        
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
        	getLogger().warning("<Metrics> Failed to start MetricsLite!");
        }
        
        this.getLogger().info("Plugin Enabled Successfully");
	}
	
	/**
	 * Called when this plug-in is disabled 
	 */
	@Override
	public void onDisable(){
		this.getLogger().info("Plugin Disabled Successfully");
	}
	
	/**
	 * Executes the given command, returning its success 
	 * 
	 * @param sender Source of the command
	 * @param cmd    Command which was executed
	 * @param label  Alias of the command which was used
	 * @param args   Passed command arguments 
	 * @return		 true if a valid command, otherwise false
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String command = cmd.getName().toLowerCase();

		// gpvault command
		if(command.equalsIgnoreCase("gpfvault")) {
			if (args.length != 1) { return false; }
			if (args[0].equalsIgnoreCase("reload")) {
				this.reloadConfig();
				messages.reloadCustomConfig();
				this.getLogger().info("Configuration Reloaded");
				if (sender instanceof Player) {
					sender.sendMessage(EMessages.Reload.get());
				}
				return true;
			}
		} 
		
		if (command.equalsIgnoreCase("setflagprice")) {
			return setPrice(EPurchaseType.Flag, sender, args);
		}
		
		if (command.equalsIgnoreCase("getflagprice")) {
			return getPrice(EPurchaseType.Flag, sender, args);
		}

		if (command.equalsIgnoreCase("setmessageprice")) {
			return setPrice(EPurchaseType.Message, sender, args);
		}
		
		if (command.equalsIgnoreCase("getmessageprice")) {
			return getPrice(EPurchaseType.Message, sender, args);
		}
		
		if (command.equalsIgnoreCase("setflagrefund")) {
			return setRefundable(EPurchaseType.Flag, sender, args);
		}
		
		if (command.equalsIgnoreCase("setmessagerefund")) {
			return setRefundable(EPurchaseType.Message, sender, args);
		}
		
		if (command.equalsIgnoreCase("previewmessage")) {
			return showMessage(sender, args);
		}
		return false;
	}
	
	/**
	 * Register with the Vault economy plugin.
	 * 
	 * @return True if the economy was successfully configured. 
	 */
    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
        		.getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
        	economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	/**
	 * Sets the price of a flag or message.
	 * 
	 * @param type Flag or Message
	 * @param sender The command sender
	 * @param args The command arguments
	 * @return true if a valid command, otherwise false
	 */
	private boolean setPrice(EPurchaseType type, CommandSender sender, String[] args) {
		if (args.length != 2) { return false; }
		
		// Check to see that the flag is valid
		Flag flag = new Flag();
		if (!flag.setType(args[0])) {
			sender.sendMessage(Messages.InvalidFlagError.getMessage()
					.replaceAll("<2>", args[0])
					.replaceAll("<10>", EPurchaseType.Flag.getLocal().toLowerCase()));
			return true; 
		}
		
		// Check to see that the price is valid
		try { Double.valueOf(args[1]); }
		catch (NumberFormatException ex) {
			sender.sendMessage(EMessages.PriceError.get()
					.replaceAll("<1>", args[1]));
			return true;
		}
		
		// Set the new price
		this.getConfig().set("Price." + type.toString() + "." + flag.getType().toString(), args[1]);
		this.saveConfig();
				
		// Send the message
		sender.sendMessage(EMessages.SetPrice.get()
				.replaceAll("<0>", type.getLocal().toLowerCase())
				.replaceAll("<1>", economy.format(Double.valueOf(args[1])))
				.replaceAll("<2>", flag.getType().getLocalName()));
		return true;
	}
	
	/**
	 * Gets the price of a flag or message.
	 * 
	 * @param type Flag or Message
	 * @param sender The command sender
	 * @param args The command arguments
	 * @return true if a valid command, otherwise false
	 */
	private boolean getPrice(EPurchaseType type, CommandSender sender, String[] args) {
		if (args.length != 1) { return false; }
		
		// Check to see that the flag is valid
		Flag flag = new Flag();
		if (!flag.setType(args[0])) {
			sender.sendMessage(Messages.InvalidFlagError.getMessage()
					.replaceAll("<2>", args[0])
					.replaceAll("<10>", EPurchaseType.Flag.getLocal().toLowerCase()));
			return true; 
		}

		// Get the message
		String setting = GPFVault.instance.getConfig()
				.getString("Price." + type.toString() + "." + flag.getType().toString());
		
		// Verify the stored price is valid
		String formattedCost = messages.getCustomConfig().getString("Invalid");
		try {
			double cost = 0;
			if (setting != null) {
				cost = Double.valueOf(setting);
			}
			formattedCost = economy.format(Double.valueOf(cost));
		} finally {
			// Send the message
			sender.sendMessage(EMessages.GetPrice.get()
					.replaceAll("<0>", type.getLocal().toLowerCase())
					.replaceAll("<1>", formattedCost)
					.replaceAll("<2>", flag.getType().getLocalName()));
		}
		return true;
	}
	
	/**
	 * Displays a formatted player flag message
	 * 
	 * @param sender The command sender
	 * @param args   The command arguments
	 * @return true if a valid command, otherwise false
	 */
	private static boolean showMessage(CommandSender sender, String[] args) {
		// Make sure this is a player, we need claim information
		if(!(sender instanceof Player)) { 
			sender.sendMessage(Messages.NoConsoleError.getMessage());
			return true;
		}
		Player player = (Player) sender;
		
		// Acquire the claim
		Claim claim = ClaimManager.getClaimAtLocation(player.getLocation());
		if (claim == null) { 
			sender.sendMessage(Messages.NoClaimError.getMessage());
			return true;
		}
		
		// Build the message from the remaining arguments
		StringBuilder message = new StringBuilder();
		for (int x = 0; x < args.length; x++) {

			message.append(args[x]);
			if (x < args.length - 1) {
				message.append(" ");
			}
		}
		
		// Send the message
		sender.sendMessage(message.toString()
				.replaceAll("<0>", player.getName())
				.replaceAll("<1>", claim.getOwnerName()));
		return true;
	}

	/**
	 * Change the refund state in config.yml
	 * 
	 * @param type The product type to change
	 * @param sender The CommandSender
	 * @param args The arguments (there shouldn't be any)
	 * @return true if a valid command, otherwise false
	 */
	private boolean setRefundable(EPurchaseType type, CommandSender sender, String[] args) {
		if (args.length != 0) { return false; }
		GPFVault.instance.getConfig().set("Refund." + type.toString(), !type.isRefundable());
		GPFVault.instance.saveConfig();
		String value = Messages.ValueColorTrue.getMessage().toLowerCase();
		if(!type.isRefundable()) {
			value = Messages.ValueColorFalse.getMessage().toLowerCase();
		}
	
		sender.sendMessage(EMessages.RefundState.get()
				.replaceAll("<0>", type.toString().toLowerCase())
				.replaceAll("<4>", value));
		return true;
	}
	
    /**
     * Send a debug message to the console
     * 
     * @param message The debug message
     */
    protected void Debug(String message) {
    	if (DEBUG) {
    		this.getLogger().info("<Debug> " + message);
    	}
    }
    
}
