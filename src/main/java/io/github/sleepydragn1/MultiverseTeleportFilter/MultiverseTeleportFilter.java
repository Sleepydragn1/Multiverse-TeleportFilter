/******************************************************************************
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.                       *
 * Multiverse 2 is licensed under the BSD License.                            *
 * For more information please check the README.md file included              *
 * with this project.                                                         *
 ******************************************************************************/

/******************************************************************************
 * Multiverse-TeleportFilter was developed by Sleepydragn1.                   *
 * 																			  *
 * Source can be found at 													  *
 * https://github.com/Sleepydragn1/Multiverse-TeleportFilter.				  *
 * 																			  *
 * Report any/all bugs on Github, and you can email me directly at 			  *
 * sleepydragon10@hotmail.com for other suggestions or concerns if needed.    *
 * 																			  *
 * While this plugin depends on significant portions of Multiverse-Core, it   *
 * was developed independently of the Multiverse Team. As such, it should not *
 * be taken as a reflection of their quality of work.						  *
 * 																			  *
 * AKA - I'm a novice Java/Bukkit coder, and I wouldn't want to tarnish		  *
 * their reputation, especially considering how nice their current code-base  *
 * is.																		  *
 ******************************************************************************/

package io.github.sleepydragn1.MultiverseTeleportFilter;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Server;
import org.bukkit.event.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements MVPlugin, Listener {
	MultiverseTeleportFilter plugin;
	Server bukkit = getServer();
	
	private MultiverseCore core;
	private MVWorldManager multiverseworldmanager = null;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[Multiverse-TeleportFilter] ";
	private static final int PROTOCOL = 1;
	private FileConfiguration config;
	
	// Soft disable flag (used in "/mvtpf disable" and "/mvtpf enable")
	public boolean softDisable = false;
	
	// Called when the plugin is enabled
	@Override    
	public void onEnable() {
    	plugin = this;
		
		// Retrieves the configuration file, loads its entries for later use
		plugin.saveDefaultConfig();
    	plugin.config = plugin.getConfig();
    	
    	// Checks if the "enabled" option in config.yml is true/false and acts accordingly
		if (!config.getBoolean("options.enabled")) {
    		log(Level.INFO, "Multiverse-Teleport filter has been disabled by config.yml. Perhaps you should uninstall it instead?");
    		log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
    		plugin.setEnabled(false);
    		return;
    	}
	
		this.core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		// Checks if Multiverse-Core is running
		if (plugin.core == null) {
			log(Level.WARNING, "Multiverse-Core has not been detected running.");
			log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
			plugin.setEnabled(false);
			return;
    	}
		
		multiverseworldmanager = core.getMVWorldManager();
		
		// Checks if Multiverse-TeleportFilter is coded with the server's version of Multiverse-Core in mind
		if (core.getProtocolVersion() != 18) {
			if (!config.getBoolean("options.ignore-core-version-check")) {
				log(Level.SEVERE, "Multiverse-Core has been updated past Multiverse-TeleportFilter, so Multiverse-TeleportFilter will be disabled. Ask the plugin author to update the plugin, or enable \"ignore-core-version\" in config.yml!");
				log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
				plugin.setEnabled(false);
				return;
			}
			else {
				log(Level.SEVERE, "Multiverse-Core has been updated past Multiverse-TeleportFilter, but \"ignore-core-version-check\" is enabled in config.yml. Ask the plugin author to update the plugin!");
			}
		}
    	log(Level.INFO, "enabled.");
    	plugin.core.incrementPluginCount();
    	
    	getServer().getPluginManager().registerEvents(plugin, plugin);
    }
		
	// Called when a MVTeleportEvent happens (i.e. someone attempts to teleport somewhere using Multiverse)
	@EventHandler
	public void onTP(MVTeleportEvent e) {	
		// Checks if the softDisable flag is true (if "/mvtpfdisable" has been run, it will be true), and then stops any filter
		// checking if it is true
		if (plugin.softDisable) return;
		
		Player teleportee; 
		CommandSender teleporter;
		String originName, fancyTextOriginName, destinationName, fancyTextDestinationName;
		
		teleporter = e.getTeleporter();
		teleportee = e.getTeleportee();
		
		// originName and destinationName are the plaintext origin world name and destination world name and are used in comparing
		// the world names to the filter's retrieved list.
		// The fancyText versions of these two variables include the coloring/stylizing for the world names as set by
		// Multiverse-Core's worlds.yml file, and are used in messages to the player.
		originName = teleportee.getWorld().getName();
		fancyTextOriginName = multiverseworldmanager.getMVWorld(originName).getName();
		destinationName = e.getDestination().getLocation(null).getWorld().getName();
		fancyTextDestinationName = e.getDestination().getName();
		
		// Allows the console to bypass the teleportFilter check
		if (teleporter instanceof Player) {
			// Used in wildcard filter situations
			if (teleportFilter(teleportee, originName, destinationName) == 1) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + ".");
				return;
			}
			// Used in specific filter situations
			if (teleportFilter(teleportee, originName, destinationName) == 2) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + " when in " + fancyTextOriginName + ".");
				return;
			}
		}
		return;
	}
	
	// Here's the actual teleport filtering method
	public int teleportFilter(Player teleportee, String originName, String destinationName) {
		// returns 0 ----> Teleportee is allowed to make the teleport
		// returns 1 ----> Teleportee's teleport is denied based on a wildcard filter (i.e. all teleports to that destination world are denied)
		// returns 2 ----> Teleportee's teleport is denied based on a specific filter (i.e. teleports from this specific origin world to the
		// destination is denied)
		if (config.getStringList("teleportfilter." + destinationName).contains(originName)) {
			if (config.getBoolean("options.ignore-permissions")) return 1;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
	        {	
	        	if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 1;
	        }
			else return 0;
		}
		if (config.getStringList("teleportfilter." + destinationName).contains("all") || config.getStringList("teleportfilter." + destinationName).contains("wildcard")) {
			if (config.getBoolean("options.ignore-permissions")) return 2;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
	        {	
	        	if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 2;
	        }
			else return 0;
		}
		else return 0;
	}
	
	// Simple reload command that reloads the config.yml file and also checks if the "enabled" option has changed
	public void reload() {
		plugin.reloadConfig();
		plugin.config = plugin.getConfig();
		
		// Checks if the "enabled" option in config.yml is true/false and acts accordingly
		if (!config.getBoolean("options.enabled")) {
    		log(Level.INFO, "Multiverse-Teleport filter has been disabled by config.yml. Perhaps you should uninstall it instead?");
    		log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
    		plugin.setEnabled(false);
    		return;
    	}	
	}
	
	// Here be MVTPF's commands
	@Override
	// Ignores Bukkit's deprecation flag on getPlayer(), which is needed in this instance
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Reload command
		if (cmd.getName().equalsIgnoreCase("mvtpfreload") && (sender.hasPermission("multiverse.teleportfilter.reload") || (!(sender instanceof Player)))) { 
			plugin.reload();
			return true;
		}
		// Disable command
		if (cmd.getName().equalsIgnoreCase("mvtpfdisable") && (sender.hasPermission("multiverse.teleportfilter.disable") || (!(sender instanceof Player)))) {
			if (softDisable = false) {
				plugin.softDisable = true;
				config.set("options.soft-disable", true);
				log(Level.INFO,"Teleport filter disabled!");
			}
			else sender.sendMessage("The teleport filter was already disabled!");
			return true;
		}
		// Enable command
		if (cmd.getName().equalsIgnoreCase("mvtpfenable") && (sender.hasPermission("multiverse.teleportfilter.enable") || (!(sender instanceof Player)))) {
			if (softDisable = false) {
				plugin.softDisable = true;
				config.set("options.soft-disable", false);
				log(Level.INFO,"Teleport filter enabled!");
			}
			else sender.sendMessage("The teleport filter was already enabled!");
			return true;
		}
		// Status command
		if (cmd.getName().equalsIgnoreCase("mvtpfstatus") && (sender.hasPermission("multiverse.teleportfilter.status") || (!(sender instanceof Player)))) {
			if (softDisable) sender.sendMessage("The teleport filter is currently disabled.");
			if (!softDisable) sender.sendMessage("The teleport filter is currently enabled.");
			else sender.sendMessage("The teleport filter has become Schrödinger's cat, please contact the plugin author accordingly.");
			return true;
		}
		// Allowed command
		if (cmd.getName().equalsIgnoreCase("mvtpfallowed") && (sender.hasPermission("multiverse.teleportfilter.allowed") || (!(sender instanceof Player)))) {
			Player player;
			String originName, destinationName;
			// Flag indicates whether or not the sender is checking themselves via the command
			Boolean personalFlag = false;
				
			// Checks if the sender is a player or console, acts accordingly
			if (sender instanceof Player) {
				if (bukkit.getPlayer(args[0]) != null) {
					player = bukkit.getPlayer(args[0]);
					if ((Player) sender == player) personalFlag = true;
				}
				else {
					player = (Player) sender;
					personalFlag = true;
				}
			}
			else {
				if (bukkit.getPlayer(args[0]) != null) player = bukkit.getPlayer(args[0]);
				else return false;
			}
				
			// Checks for number of arguments and validity of the specified worlds, and acts accordingly
			if (bukkit.getWorld(args[1]) != null) {
				if (bukkit.getWorld(args[2]) != null) { 
					originName = bukkit.getWorld(args[1]).getName();
					destinationName = bukkit.getWorld(args[2]).getName();
				}
				else {
					originName = player.getWorld().getName();
					destinationName = bukkit.getWorld(args[1]).getName();
				}
			}
			else {
				sender.sendMessage(args[1] + " is not a valid world!");
				return false;
			}
				
			// Return of the Fancy Text names used in sendMessage()
			String fancyTextOriginName = multiverseworldmanager.getMVWorld(originName).getName();
			String fancyTextDestinationName = multiverseworldmanager.getMVWorld(destinationName).getName();
				
			// Checks if the specified player is allowed to do the specified teleport, then reports back.
			if (!personalFlag) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage(player.getName() + " can teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
					case 1: sender.sendMessage(player.getName() + " cannot teleport to " + fancyTextDestinationName + " from any world.");
					case 2: sender.sendMessage(player.getName() + " cannot teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
				}
				return true;
			}
			if (personalFlag) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage ("You can teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
					case 1: sender.sendMessage("You cannot teleport to " + fancyTextDestinationName + " from any world.");
					case 2: sender.sendMessage("You cannot teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
				}
				return true;
			}
				
			return false;
		}
		// Filter Add Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("add") && (args[1] != null) && (args[2] != null) && (sender.hasPermission("multiverse.teleportfilter.filter.add") || (!(sender instanceof Player)))) {
			if (bukkit.getWorld(args[1]) != null) {
				if ((bukkit.getWorld(args[2]) != null) || (args[2] == "all") || (args[2] == "wildcard")) {
					String destinationName = args[1];
					String originName = args[2];
					String fancyTextOriginName = multiverseworldmanager.getMVWorld(originName).getName();
					String fancyTextDestinationName = multiverseworldmanager.getMVWorld(destinationName).getName();
						
					List<String> configList;
						
					if (config.getStringList("teleportfilter." + destinationName) == null) {
						configList = new ArrayList<String>();
						configList.add(originName);
					}
					else {
						configList = config.getStringList("teleportfilter." + destinationName);
						if (configList.contains(originName)) {
							sender.sendMessage("That filter entry already exists!");
							return true;
						}
						if (configList.contains("all") || configList.contains("wildcard")) {
							sender.sendMessage("All teleports to this destination are already blocked via a wildcard filter entry.");
							return true;
						}
					}
					config.set("teleportfilter." + destinationName, configList);
					if (sender instanceof Player) sender.sendMessage("Filter entry for destination world " + fancyTextDestinationName + " and origin world " + fancyTextOriginName + " successfully added.");
					log(Level.INFO, "Filter entry for destination world " + destinationName + " and origin world " + originName + " successfully added.");
					return true;
				}
				else {
					sender.sendMessage(args[2] + " is not a valid world!");
					return true;
				}
			}
			else {
				sender.sendMessage(args[1] + " is not a valid world!");
				return true;
			}
		}
		// Filter Remove Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("remove") && (args[1] != null) && (args[2] != null) && (sender.hasPermission("multiverse.teleportfilter.filter.remove") || (!(sender instanceof Player)))) {
			String destinationName = args[1];
			String originName = args[2];
			List<String> configList;
						
			if (config.getStringList("teleportfilter." + destinationName) == null) {
				sender.sendMessage(destinationName + " doesn't have a filter entry.");
				return true;
			}
			else {
				configList = config.getStringList("teleportfilter." + destinationName);
				if (!configList.contains(originName)) {
					sender.sendMessage(originName + " doesn't have a filter entry under " + destinationName + ".");
					return true;
				}
				else {
					configList.add(originName);
					config.set("teleportfilter." + destinationName, configList);
					if (sender instanceof Player) sender.sendMessage("Filter entry for origin world " + originName + "under destination world " + destinationName + " successfully removed.");
					log(Level.INFO, "Filter entry for origin world " + originName + "under destination world " + destinationName + " successfully removed.");
					return true;
				}
			}
		}
		return false;
	}
	
	// Standard method overrides needed when extending MVPlugin
	@Override
	public void log(Level level, String msg) {
		log.log(level, logPrefix + msg);
	}

	@Deprecated
	@Override
	public String dumpVersionInfo(String buffer) {
		return null;
	}

	@Override
	public MultiverseCore getCore() {
		return plugin.core;
	}

	@Override
	public int getProtocolVersion() {
		return PROTOCOL;
	}

	@Override
	public void setCore(MultiverseCore core) {
		plugin.core = core;
	}
}
