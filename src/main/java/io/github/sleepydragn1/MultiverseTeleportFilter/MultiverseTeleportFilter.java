/******************************************************************************
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.                       *
 * Multiverse 2 is licensed under the BSD License.                            *
 * For more information please check the README.md file included              *
 * with Multiverse-Core.                                                      *
 ******************************************************************************/

/******************************************************************************
 * Multiverse-TeleportFilter was developed by Sleepydragn1.                   *
 *                                                                            *
 * Source can be found at                                                     *
 * https://github.com/Sleepydragn1/Multiverse-TeleportFilter.                 *
 *                                                                            *
 * Report any/all bugs on Github, and you can email me directly at            *
 * sleepydragon10@hotmail.com for other suggestions or concerns if needed.    *
 *                                                                            *
 * While this plugin depends on significant portions of Multiverse-Core, it   *
 * was developed independently of the Multiverse Team, and does not           *
 * redistribute any of their code.                                            *
 *                                                                            *
 * I'm a novice Java/Bukkit coder, so expect mistakes, odd coding that        *
 * doesn't match normal coding conventions, inefficiencies, bugs, and other   *
 * maladies.                                                                  *
 ******************************************************************************/

// TODO: Create javadocs for methods
// TODO: Investigate removal of colored world names where applicable
// TODO: Allowed command
// TODO: Make config reload also work with /mv reload
// TODO: Proper paging for /mvtpffilter list
// TODO: Investigate using both aliases and normal world names (why does it work in the config?)

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

import io.github.sleepydragn1.MultiverseTeleportFilter.TabText;

public final class MultiverseTeleportFilter extends JavaPlugin implements MVPlugin, Listener {
	MultiverseTeleportFilter plugin;
	Server bukkit = getServer();
	
	private MultiverseCore core;
	private MVWorldManager multiverseworldmanager;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[Multiverse-TeleportFilter] ";
	private static final int PROTOCOL = 1;
	private FileConfiguration config;
	
	// Filter disable flag (used in "/mvtpf disable" and "/mvtpf enable")
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
			log(Level.INFO,"Filter disabled due to configuration!");
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
		String originName, coloredOriginName, destinationName, coloredDestinationName;
		
		teleporter = e.getTeleporter();
		teleportee = e.getTeleportee();
		
		// originName and destinationName are the plaintext origin world name and destination world name and are used in comparing
		// the world names to the filter's retrieved list.
		// The colored versions of these two variables include the coloring/stylizing for the world names as set by
		// Multiverse-Core's worlds.yml file, and are used in messages to the player.
		coloredOriginName = multiverseworldmanager.getMVWorld(teleportee.getWorld()).getColoredWorldString();
		originName = ChatColor.stripColor(coloredOriginName);
		coloredDestinationName = e.getDestination().getName();
		destinationName = ChatColor.stripColor(coloredDestinationName);
		
		// Allows the console to bypass the teleportFilter check
		if (teleporter instanceof Player) {
			// Used in wildcard filter situations
			if (teleportFilter(teleportee, originName, destinationName) == 1) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + coloredDestinationName + ".");
				return;
			}
			// Used in specific filter situations
			if (teleportFilter(teleportee, originName, destinationName) == 2) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + coloredDestinationName + " when in " + coloredOriginName + ".");
				return;
			}
		}
		return;
	}
	
	// The actual teleport filtering method
	public int teleportFilter(Player teleportee, String originName, String destinationName) {
		// returns 0 ----> Teleportee is allowed to make the teleport
		// returns 1 ----> Teleportee's teleport is denied based on a wildcard filter (i.e. all teleports to that destination world are denied)
		// returns 2 ----> Teleportee's teleport is denied based on a specific filter (i.e. teleports from this specific origin world to the
		// destination is denied)
		
		// Allows Ops to bypass the teleport filter if "ye-olde-op-system" is true.
		if (config.getBoolean("options.ye-olde-op-system") && teleportee.isOp()) return 0;
		
		if (config.getStringList("teleportfilter." + destinationName).contains("all") || config.getStringList("teleportfilter." + destinationName).contains("wildcard")) {
			if (config.getBoolean("options.ignore-filter-permissions")) return 1;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass")) {	
	        	if (teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if (teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 1;
	        }
			else return 0;
		}
		if (config.getStringList("teleportfilter." + destinationName).contains(originName)) {
			if (config.getBoolean("options.ignore-filter-permissions")) return 2;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass")) {	
	        	if (teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if (teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
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
		
		// Checks if the "filter-disabled" option in config.yml is true/false and acts accordingly
		if (config.getBoolean("options.filter-disabled")) {
			plugin.filterDisable = true;
			log(Level.INFO,"Filter disabled due to configuration change!");
    	}
		if ((!config.getBoolean("options.filter-disabled"))) {
			plugin.filterDisable = false;
			log(Level.INFO,"Filter enabled due to configuration change!");
    	}	
	}
	
	// Here be MVTPF's commands
	@Override
	// Ignores Bukkit's deprecation flag on getPlayer(), which is needed in this instance
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Parent command/status command - indicates the plugin is running and gives the current version
		if (cmd.getName().equalsIgnoreCase("mvtpf")) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.info")) return true;
			
			sender.sendMessage("Running Multiverse-TeleportFilter version " + plugin.getDescription().getVersion() + "!");
			// Indicates whether or not the filter is enabled
			if (filterDisable) sender.sendMessage("The teleport filter is currently disabled.");
			if (!filterDisable) sender.sendMessage("The teleport filter is currently enabled.");
			return true;
		}
		
		// Reload command
		if (cmd.getName().equalsIgnoreCase("mvtpfreload")) { 
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.reload")) return true;
			
			plugin.reload();
			sender.sendMessage("Plugin configuration reloaded!");
			return true;
		}
		
		// Disable command
		if (cmd.getName().equalsIgnoreCase("mvtpfdisable")) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.disable")) return true;
			
			if (!plugin.filterDisable) {
				plugin.filterDisable = true;
				config.set("options.filter-disabled", true);
				plugin.saveConfig();
				if (sender instanceof Player) sender.sendMessage("Teleport filter disabled!");
				log(Level.INFO,"Teleport filter disabled!");
			}
			else sender.sendMessage("The teleport filter was already disabled!");
			return true;
		}
		
		// Enable command
		if (cmd.getName().equalsIgnoreCase("mvtpfenable") ) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.enable")) return true;
			
			if (plugin.filterDisable) {
				plugin.filterDisable = false;
				config.set("options.filter-disabled", false);
				plugin.saveConfig();
				if (sender instanceof Player) sender.sendMessage("Teleport filter enabled!");
				log(Level.INFO,"Teleport filter enabled!");
			}
			else sender.sendMessage("The teleport filter was already enabled!");
			return true;
		}
		
		// Status command
		if (cmd.getName().equalsIgnoreCase("mvtpfstatus")) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.status")) return true;
			
			if (filterDisable) sender.sendMessage("The teleport filter is currently disabled.");
			if (!filterDisable) sender.sendMessage("The teleport filter is currently enabled.");
			else sender.sendMessage("The teleport filter has become Schrödinger's cat, please contact the plugin author accordingly.");
			return true;
		}
		
		// Declaration of variables shared between commands due to naming standardization
		String originName, destinationName, coloredDestinationName, coloredOriginName;
		
		// Allowed command
		if (cmd.getName().equalsIgnoreCase("mvtpfallowed") && (args.length > 0)) {	
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.allowed")) return true;
			
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
						coloredDestinationName = multiverseworldmanager.getMVWorld(args[1]).getColoredWorldString();
						coloredOriginName = multiverseworldmanager.getMVWorld(args[2]).getColoredWorldString();
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
						coloredDestinationName = multiverseworldmanager.getMVWorld(args[0]).getColoredWorldString();
					}
				
					if (!multiverseworldmanager.isMVWorld(args[1])) {
						sender.sendMessage(args[1] + " is not a valid world!");
						return true;
					}
					else coloredOriginName = multiverseworldmanager.getMVWorld(args[1]).getColoredWorldString();
				}
				else {
					if (!multiverseworldmanager.isMVWorld(args[0])) {
						sender.sendMessage(args[0] + " is not a valid world!");
						return true;
					}
					else {
						coloredDestinationName = multiverseworldmanager.getMVWorld(args[0]).getColoredWorldString();
						coloredOriginName = multiverseworldmanager.getMVWorld(((Player) sender).getWorld()).getColoredWorldString();
					}
				}
			}
			
			originName = ChatColor.stripColor(coloredOriginName);
			destinationName = ChatColor.stripColor(coloredDestinationName);
			
			// Checks if the specified player is allowed to do the specified teleport, then reports back.
			if (!personalFlag && (sender instanceof Player)) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage(player.getName() + " can teleport to " + coloredDestinationName + " from " + coloredOriginName + ".");
							break;
					case 1: sender.sendMessage(player.getName() + " cannot teleport to " + coloredDestinationName + " from any world.");
							break;
					case 2: sender.sendMessage(player.getName() + " cannot teleport to " + coloredDestinationName + " from " + coloredOriginName + ".");
							break;
				}
			}
			if (personalFlag && (sender instanceof Player)) {
				switch (teleportFilter(player, originName, destinationName)) {
					case 0: sender.sendMessage ("You can teleport to " + coloredDestinationName + " from " + coloredOriginName + ".");
							break;
					case 1: sender.sendMessage("You cannot teleport to " + coloredDestinationName + " from any world.");
							break;
					case 2: sender.sendMessage("You cannot teleport to " + coloredDestinationName + " from " + coloredOriginName + ".");
							break;
				}
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
			}
			return true;
		}
		
		// Parent filter command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && (args.length == 0) && ((sender.hasPermission("multiverse.teleportfilter.filter.add") || (sender.hasPermission("multiverse.teleportfilter.filter.remove"))))) {
			return false;
		}
		
		// Filter Add Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("add") && (args.length == 3)) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.add")) return true;
			
			if (multiverseworldmanager.isMVWorld(args[1])) {
				if (multiverseworldmanager.isMVWorld(args[2]) || (args[2] == "all") || (args[2] == "wildcard")) {
					coloredDestinationName = multiverseworldmanager.getMVWorld(args[1]).getColoredWorldString();
					destinationName = ChatColor.stripColor(coloredDestinationName);
					coloredOriginName = multiverseworldmanager.getMVWorld(args[2]).getColoredWorldString();
					originName = ChatColor.stripColor(coloredOriginName);
						
					List<String> configList;
						
					if (config.getStringList("teleportfilter." + destinationName) == null) {
						configList = new ArrayList<String>();
						configList.add(originName);
					}
					else {
						configList = config.getStringList("teleportfilter." + destinationName);
						if (configList.contains(originName)) sender.sendMessage("That filter entry already exists!");
						if (configList.contains("all") || configList.contains("wildcard")) sender.sendMessage("All teleports to this destination are already blocked via a wildcard filter entry.");
						configList.add(originName);
					}
					config.set("teleportfilter." + destinationName, configList);
					plugin.saveConfig();
					if (sender instanceof Player) sender.sendMessage("Filter entry for destination world " + coloredDestinationName + " and origin world " + coloredOriginName + " successfully added.");
					log(Level.INFO, "Filter entry for destination world " + destinationName + " and origin world " + originName + " successfully added.");
				}
				else {
					sender.sendMessage(args[2] + " is not a valid world!");
				}
			}
			else {
				sender.sendMessage(args[1] + " is not a valid world!");
			}
			return true;
		}
		
		// Filter Remove Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("remove") && (args.length == 3)) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.remove")) return true;
			
			destinationName = args[1];
			originName = args[2];
			List<String> configList;
						
			if (config.getStringList("teleportfilter." + destinationName).size() == 0) sender.sendMessage("The filter doesn't have an existing filter entry for destination " + destinationName + ".");
			else {
				configList = config.getStringList("teleportfilter." + destinationName);
				if (!configList.contains(originName)) sender.sendMessage("The filter doesn't have an existing entry for origin " + originName + " under destination " + destinationName + ".");
				else {
					configList.remove(originName);
					config.set("teleportfilter." + destinationName, configList);
					plugin.saveConfig();
					if (sender instanceof Player) sender.sendMessage("Filter entry for destination world " + destinationName + " and origin world " + originName + " successfully removed.");
					log(Level.INFO, "Filter entry for origin world " + originName + "under destination world " + destinationName + " successfully removed.");
				}
			}
			return true;
		}
		
		// Filter Check Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("check") && (args.length == 3)) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.check")) return true;
			
			destinationName = args[1];
			originName = args[2];
			List<String> configList;
			
			if (multiverseworldmanager.isMVWorld(args[1])) {
				if (multiverseworldmanager.isMVWorld(args[2])) {
					coloredDestinationName = multiverseworldmanager.getMVWorld(destinationName).getColoredWorldString();
					coloredOriginName = multiverseworldmanager.getMVWorld(originName).getColoredWorldString();
					configList = config.getStringList("teleportfilter." + destinationName);
					if (configList.size() > 0) {
						if (configList.contains(originName)) {
							if (sender instanceof Player) sender.sendMessage("The filter entry for destination world " + coloredDestinationName + " and origin world " + coloredOriginName + " exists.");
							else sender.sendMessage("The filter entry for destination world " + destinationName + " and origin world " + originName + " exists.");
						}
						else {
							if (sender instanceof Player) sender.sendMessage("The filter entry for destination world " + coloredDestinationName + " and origin world " + coloredOriginName + " does not exist.");
							else sender.sendMessage("The filter entry for destination world " + destinationName + " and origin world " + originName + " does not exist.");
						}
					}
					else {
						if (sender instanceof Player) sender.sendMessage("No filter entry for destination world " + coloredDestinationName + " exists.");
						else sender.sendMessage("No filter entry for destination world " + destinationName + " exists.");
					}
				}
				else sender.sendMessage(originName + " is not a valid world!");
			}
			else sender.sendMessage(destinationName + " is not a valid world!");
			return true;
		}
		
		// Filter List Command
		if (cmd.getName().equalsIgnoreCase("mvtpffilter") && args[0].equalsIgnoreCase("list")) {
			// Permissions check
			if (!commandPermissible(sender, "multiverse.teleportfilter.filter.list")) return true;
			
			// Grabs the page number
			int page;
			// If no page number is specified, it defaults to page one
			if (args.length < 2) page = 1; 
			else {
				try {
					page = Integer.parseInt(args[1]);
				}
				// If the second argument is not an integer, the command returns false
				catch (NumberFormatException e) {
					if (args[1] == "all") page = 0;
					else return false;
				}
			}

			// Grabs the destinations in the form of a Set<String>, then converts them to list form
			List<String> destinations = new ArrayList<>();
			for (String destination : config.getConfigurationSection("teleportfilter").getKeys(false)) destinations.add(destination);
			
			Integer destinationsSize = destinations.size();
			
			// If there are no filter rules, this runs
			if (destinationsSize == 0) {
				sender.sendMessage("There are no teleport filter entries!");
				return true;
			}
			
			String forDestination, forOrigin, coloredForDestination, coloredForOrigin, forDestinationColorString, forOriginColorString, forDestinationSpacing, listString, titleString;
			List<String> forOriginsList;
			Integer line, forDestinationLength, forOriginLength;
			TabText tt1, tt2, tt3;
			ChatColor forDestinationColor, forOriginColor;
			
			line = 0;
			listString = "";
			titleString = ChatColor.ITALIC + "" + ChatColor.DARK_BLUE + "Destination:`" + ChatColor.RESET + ChatColor.YELLOW + "| " + ChatColor.ITALIC + "" + ChatColor.GREEN + "Origin:" + ChatColor.RESET + ChatColor.YELLOW + "\n---------------------------------------------------------------------------------------------------`---------------------------------------------------------------------------------------------------";
			
			for (int i=0; i<destinationsSize; i++) {
				forDestination = destinations.get(i);
				// Checks the validity of the destination
				if (multiverseworldmanager.isMVWorld(forDestination)) {
					forOriginsList = config.getStringList("teleportfilter." + forDestination);
					for (int k=0; k<forOriginsList.size(); k++) {
						line = line + 1;
						forOrigin = forOriginsList.get(k);
						// Checks the validity of the origin
						if (multiverseworldmanager.isMVWorld(forOrigin)) {
							forDestinationLength = forDestination.length();
							forOriginLength = forOrigin.length();
							
							// Grabs the worlds' colors
							coloredForDestination = multiverseworldmanager.getMVWorld(forDestination).getColoredWorldString();
							coloredForOrigin = multiverseworldmanager.getMVWorld(forOrigin).getColoredWorldString();
							
							if ((i > 0) || (k > 0)) listString = listString + "\n";
							
							if (k == 0) listString = listString + coloredForDestination + "`" + ChatColor.YELLOW + "| " + coloredForOrigin;
							else listString = listString + "`" + ChatColor.YELLOW + "| " + coloredForOrigin;
							
							if (k == (forOriginsList.size() - 1)) listString = listString + "\n" +  ChatColor.YELLOW + "---------------------------------------------------------------------------------------------------`---------------------------------------------------------------------------------------------------";
						}
					}
				}
			}
			
			tt1 = new TabText(titleString);
			tt1.setTabs(new int[]{23});
			
			tt2 = new TabText(listString);
			tt2.setTabs(new int[]{23});
			
			if (page == 1) sender.sendMessage(tt1.getPage(1));
			sender.sendMessage(tt2.getPage(page));

			return true;
		}
		
		return false;
	}
		
	// Permission checking method for commands
	public boolean commandPermissible(CommandSender sender, String permission) {
		// If the method returns true, the player is allowed to run the command
		// If the method returns false, the player is not allowed to run the command, and a message reflecting this
		// is issued to them.
		
		// System for allowing those not using permissions to still use the plugin's commands (if they're an Op)
		if (config.getBoolean("options.ye-olde-op-system")) {
			if (!(sender instanceof Player)) return true;
			if (sender.isOp()) return true;
			else {
				sender.sendMessage("You need to be an Op to run this command.");
				return false;
			}
		}
		// If the sender is a player, this runs
		if (sender instanceof Player) {
			if (sender.hasPermission(permission) || sender.hasPermission("multiverse.teleportfilter.*")) return true;
			else {
				sender.sendMessage("You need " + ChatColor.GRAY + permission + ChatColor.WHITE + " to run this command.");
				return false;
			}
		}
		// If the sender is the console or something else, it returns true
		else return true;
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
	
	// This plugin will save children, but not the British children.
}
