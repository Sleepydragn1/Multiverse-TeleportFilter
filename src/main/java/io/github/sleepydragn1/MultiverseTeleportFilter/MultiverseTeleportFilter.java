/******************************************************************************
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.                       *
 * Multiverse 2 is licensed under the BSD License.                            *
 * For more information please check the README.md file included              *
 * with Multiverse-Core.                                                      *
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
 * was developed independently of the Multiverse Team, and does not           *
 * redistribute any of their code.					                          *
 * 																			  *
 * I'm a novice Java/Bukkit coder, so expect mistakes, odd coding that
 * doesn't match normal coding conventions, inefficiencies, bugs, and other
 * maladies.
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
import org.bukkit.ChatColor;

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
	public boolean filterDisable = false;
	
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
    	
		// Checks if the "filter-disabled" option in config.yml is true/false and acts accordingly
		if (config.getBoolean("options.filter-disabled")) {
			plugin.filterDisable = true;
			log(Level.INFO,"Teleport filter soft disabled due to configuration!");
    	}
    	
    	getServer().getPluginManager().registerEvents(plugin, plugin);
    }
		
	// Called when a MVTeleportEvent happens (i.e. someone attempts to teleport somewhere using Multiverse)
	@EventHandler
	public void onTP(MVTeleportEvent e) {	
		// Checks if the filterDisable flag is true (if "/mvtpfdisable" has been run, it will be true), and then stops any filter
		// checking if it is true
		if (plugin.filterDisable) return;
		
		Player teleportee; 
		CommandSender teleporter;
		String originName, fancyTextOriginName, destinationName, fancyTextDestinationName;
		
		teleporter = e.getTeleporter();
		teleportee = e.getTeleportee();
		
		// originName and destinationName are the plaintext origin world name and destination world name and are used in comparing
		// the world names to the filter's retrieved list.
		// The fancyText versions of these two variables include the coloring/stylizing for the world names as set by
		// Multiverse-Core's worlds.yml file, and are used in messages to the player.
		fancyTextOriginName = multiverseworldmanager.getMVWorld(teleportee.getWorld()).getName();
		originName = ChatColor.stripColor(fancyTextOriginName);
		fancyTextDestinationName = e.getDestination().getName();
		destinationName = ChatColor.stripColor(fancyTextDestinationName);
		
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
			if (config.getBoolean("options.ignore-filter-permissions")) return 1;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass")) {	
	        	if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 1;
	        }
			else return 0;
		}
		if (config.getStringList("teleportfilter." + destinationName).contains("all") || config.getStringList("teleportfilter." + destinationName).contains("wildcard")) {
			if (config.getBoolean("options.ignore-filter-permissions")) return 2;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass")) {	
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
    	}	
		
		// Checks if the "soft-disable" option in config.yml is true/false and acts accordingly
		if (config.getBoolean("options.soft-disable")) {
			plugin.filterDisable = true;
			log(Level.INFO,"Teleport filter soft disabled due to configuration change!");
    	}
		if ((!config.getBoolean("options.soft-disable"))) {
			plugin.filterDisable = false;
			log(Level.INFO,"Teleport filter soft enabled due to configuration change!");
    	}	
	}
	
	// Here be MVTPF's commands
	@Override
	// Ignores Bukkit's deprecation flag on getPlayer(), which is needed in this instance
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Parent command/status command
		if (cmd.getName().equalsIgnoreCase("mvtpf")) {
			// Indicates it's running and gives the current version
			sender.sendMessage("Running Multiverse-TeleportFilter version " + plugin.getDescription().getVersion() + "!");
			// Indicates whether or not the filter is enabled
			if (filterDisable) sender.sendMessage("The teleport filter is currently disabled.");
			if (!filterDisable) sender.sendMessage("The teleport filter is currently enabled.");
			return true;
		}
		// Reload command
		if (cmd.getName().equalsIgnoreCase("mvtpfreload")) { 
			plugin.reload();
			sender.sendMessage("Plugin configuration reloaded!");
			return true;
		}
		// Disable command
		if (cmd.getName().equalsIgnoreCase("mvtpfdisable")) {
			if (!plugin.filterDisable) {
				plugin.filterDisable = true;
				config.set("options.soft-disable", true);
				plugin.saveConfig();
				if (sender instanceof Player) sender.sendMessage("Teleport filter disabled!");
				log(Level.INFO,"Teleport filter disabled!");
			}
			else sender.sendMessage("The teleport filter was already disabled!");
			return true;
		}
		// Enable command
		if (cmd.getName().equalsIgnoreCase("mvtpfenable")) {
			if (plugin.filterDisable) {
				plugin.filterDisable = false;
				config.set("options.soft-disable", false);
				plugin.saveConfig();
				if (sender instanceof Player) sender.sendMessage("Teleport filter enabled!");
				log(Level.INFO,"Teleport filter enabled!");
			}
			else sender.sendMessage("The teleport filter was already enabled!");
			return true;
		}
		// Status command
		if (cmd.getName().equalsIgnoreCase("mvtpfstatus")) {
			if (filterDisable) sender.sendMessage("The teleport filter is currently disabled.");
			if (!filterDisable) sender.sendMessage("The teleport filter is currently enabled.");
			else sender.sendMessage("The teleport filter has become Schrödinger's cat, please contact the plugin author accordingly.");
			return true;
		}
		
		// Declaration of variables shared between commands due to naming standardization
		String originName, destinationName, fancyTextDestinationName, fancyTextOriginName;
		
		// Allowed command
		if (cmd.getName().equalsIgnoreCase("mvtpfallowed") && (args.length > 0)) {	
			Player player;
			// Flag indicates whether or not the sender is checking themselves via the command
			Boolean personalFlag = false;
			Boolean playerFlag = false;
			
			// Player checking
			if (bukkit.getPlayer(args[0]) != null) {
				player = bukkit.getPlayer(args[0]);
				playerFlag = true;
				// Checks if the sender is specifying themselves
				try {
					if (player == (Player) sender) personalFlag = true;
				}
				// If the console is sending this command, it'll throw ClassCastException, but be caught and therefore leave
				// personalFlag to be defined by its default value — false
				catch (ClassCastException e) {
				}
			}
			else {
				// If player is meant to specified (3 or more arguments), it tells the sender that the player specified is invalid
				if (args.length > 2) {
					sender.sendMessage(args[0] + " is not a valid online player.");
					return true;
				}
				// Determines if the sender is a player or a non-player (console or otherwise)
				if (!(sender instanceof Player)) {
					sender.sendMessage(args[0] + " is not a valid online player.");
					return true;
				}
				player = (Player) sender;
				personalFlag = true;
			}
			
			// World checking
			if (playerFlag) {
				if (args.length > 2) {
					if (!multiverseworldmanager.isMVWorld(args[1])) {
						sender.sendMessage(args[1] + " is not a valid world!");
						return true;
					}
					if (!multiverseworldmanager.isMVWorld(args[2])) {
						sender.sendMessage(args[2] + " is not a valid world!");
						return true;
					}
					else {
						fancyTextDestinationName = multiverseworldmanager.getMVWorld(args[1]).getName();
						fancyTextOriginName = multiverseworldmanager.getMVWorld(args[2]).getName();
					}
				}
				else return false;
			}
			else {
				// Checks if the worlds are valid
				if (args.length > 1) {
					if (!multiverseworldmanager.isMVWorld(args[0])) {
						sender.sendMessage(args[0] + " is not a valid world!");
						return true;
					}
					else {
						fancyTextDestinationName = multiverseworldmanager.getMVWorld(args[0]).getName();
					}
				
					if (!multiverseworldmanager.isMVWorld(args[1])) {
						sender.sendMessage(args[1] + " is not a valid world!");
						return true;
					}
					else {
						fancyTextOriginName = multiverseworldmanager.getMVWorld(args[1]).getName();
					}
				}
				else {
					if (!multiverseworldmanager.isMVWorld(args[0])) {
						sender.sendMessage(args[0] + " is not a valid world!");
						return true;
					}
					else {
						fancyTextDestinationName = multiverseworldmanager.getMVWorld(args[0]).getName();
						fancyTextOriginName = multiverseworldmanager.getMVWorld(((Player) sender).getWorld()).getName();
					}
				}
			}
			
			originName = ChatColor.stripColor(fancyTextOriginName);
			destinationName = ChatColor.stripColor(fancyTextDestinationName);
			
			// Checks if the specified player is allowed to do the specified teleport, then reports back.
			if (!personalFlag && (sender instanceof Player)) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage(player.getName() + " can teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
							break;
					case 1: sender.sendMessage(player.getName() + " cannot teleport to " + fancyTextDestinationName + " from any world.");
							break;
					case 2: sender.sendMessage(player.getName() + " cannot teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
							break;
				}
				return true;
			}
			if (personalFlag && (sender instanceof Player)) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage ("You can teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
							break;
					case 1: sender.sendMessage("You cannot teleport to " + fancyTextDestinationName + " from any world.");
							break;
					case 2: sender.sendMessage("You cannot teleport to " + fancyTextDestinationName + " from " + fancyTextOriginName + ".");
							break;
				}
				return true;
			}
			else {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage (player.getName() + " can teleport to " + destinationName + " from " + originName + ".");
							break;
					case 1: sender.sendMessage(player.getName() + " cannot teleport to " + destinationName + " from any world.");
							break;
					case 2: sender.sendMessage(player.getName() + " cannot teleport to " + destinationName + " from " + originName + ".");
							break;
				}
				return true;
			}
		}
		// Parent filter command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && (args.length == 0) && ((sender.hasPermission("multiverse.teleportfilter.filter.add") || (sender.hasPermission("multiverse.teleportfilter.filter.remove"))))) {
			return false;
		}
		// Filter Add Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("add") && (args.length == 3)) {
			if (multiverseworldmanager.isMVWorld(args[1])) {
				if (multiverseworldmanager.isMVWorld(args[2]) || (args[2] == "all") || (args[2] == "wildcard")) {
					fancyTextDestinationName = multiverseworldmanager.getMVWorld(args[1]).getName();
					destinationName = ChatColor.stripColor(fancyTextDestinationName);
					fancyTextOriginName = multiverseworldmanager.getMVWorld(args[2]).getName();
					originName = ChatColor.stripColor(fancyTextOriginName);
						
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
						configList.add(originName);
					}
					config.set("teleportfilter." + destinationName, configList);
					plugin.saveConfig();
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
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("remove") && (args.length == 3)) {
			destinationName = args[1];
			originName = args[2];
			List<String> configList;
						
			if (config.getStringList("teleportfilter." + destinationName).size() == 0) {
				sender.sendMessage("The filter doesn't have an existing filter entry for destination " + destinationName + ".");
				return true;
			}
			else {
				configList = config.getStringList("teleportfilter." + destinationName);
				if (!configList.contains(originName)) {
					sender.sendMessage("The filter doesn't have an existing entry for origin " + originName + " under destination " + destinationName + ".");
					return true;
				}
				else {
					configList.remove(originName);
					config.set("teleportfilter." + destinationName, configList);
					plugin.saveConfig();
					if (sender instanceof Player) sender.sendMessage("Filter entry for destination world " + destinationName + " and origin world " + originName + " successfully removed.");
					log(Level.INFO, "Filter entry for origin world " + originName + "under destination world " + destinationName + " successfully removed.");
					return true;
				}
			}
		}
		return false;
		// Filter Check Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("check") && (args.length == 2)) {
			destinationName = args[1];
			originName = args[2];
			List<String> configList;
			
			if (multiverseworldmanager.isMVWorld(args[1])) {
				if (multiverseworldmanager.isMVWorld(args[2])) {
					fancyTextDestinationName = multiverseworldmanager.getMVWorld(destinationName).getName();
					fancyTextOriginName = multiverseworldmanager.getMVWorld(originName).getName();
					configList = config.getStringList();
					if (configList.length() > 0) {
						if (configList.contains(originName)) {
							if (sender instanceof Player) sender.sendMessage("The filter entry for destination world " + fancyTextDestinationName + " and origin world " + fancyTextOriginName + " exists.");
							else sender.sendMessage("The filter entry for destination world " + destinationName + " and origin world " + originName + " exists.");
						}
						else {
							if (sender instanceof Player) sender.sendMessage("The filter entry for destination world " + fancyTextDestinationName + " and origin world " + fancyTextOriginName + " does not exist.");
							else sender.sendMessage("The filter entry for destination world " + destinationName + " and origin world " + originName + " does not exist.");
						}
					}
					else {
						if (sender instanceof Player) sender.sendMessage("No filter entry for destination world " + fancyTextDestinationName + " exists.");
						else sender.sendMessage("No filter entry for destination world " + destinationName + " exists.");
					}
				}
				else sender.sendMessage(originName + " is not a valid world!");
			}
			else sender.sendMessage(destinationName + " is not a valid world!");
			return true;
		}
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
