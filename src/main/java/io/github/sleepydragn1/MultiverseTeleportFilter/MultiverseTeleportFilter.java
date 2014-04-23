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

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements MVPlugin, Listener {
	MultiverseTeleportFilter plugin;
	
	private MultiverseCore core;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[Multiverse-TeleportFilter] ";
	private static final int PROTOCOL = 1;
	private FileConfiguration config;
	
	// Called when the plugin is enabled
	@Override    
	public void onEnable() {
    	plugin = this;
		
		// Retrieves the configuration file, loads its entries for later use
		this.saveDefaultConfig();
    	this.config = plugin.getConfig();
    	
    	// Checks if the "enabled" option in config.yml is true/false and acts accordingly
		if (!config.getBoolean("options.enabled")) {
    		log(Level.INFO, "Multiverse-Teleport filter has been disabled by config.yml. Perhaps you should uninstall it instead?");
    		log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
    		plugin.setEnabled(false);
    		return;
    	}
    	
		this.core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		// Checks if Multiverse-Core is running
		if (this.core == null) {
			log(Level.WARNING, "Multiverse-Core has not been detected running.");
			log(Level.INFO, "Disabling Multiverse-TeleportFilter.");
			plugin.setEnabled(false);
			return;
    	}
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
    	this.core.incrementPluginCount();
    	
    	getServer().getPluginManager().registerEvents(this, this);
    }
		
	// Called when a MVTeleportEvent happens (i.e. someone attempts to teleport somewhere using Multiverse)
	@EventHandler
	public void onTP(MVTeleportEvent e) {	
		final MVWorldManager multiverseworldmanager = core.getMVWorldManager();
		
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
	
	// Below are MVTPF's commands
	
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
		return this.core;
	}

	@Override
	public int getProtocolVersion() {
		return PROTOCOL;
	}

	@Override
	public void setCore(MultiverseCore core) {
		this.core = core;
	}
}
