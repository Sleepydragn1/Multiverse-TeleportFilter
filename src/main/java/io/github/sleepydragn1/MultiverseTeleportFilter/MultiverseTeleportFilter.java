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

import org.bukkit.plugin.*;
import org.bukkit.plugin.messaging.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.permissions.*;
import org.bukkit.configuration.file.FileConfiguration;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;
import com.onarandombox.MultiverseCore.destination.WorldDestination;

public final class MultiverseTeleportFilter extends JavaPlugin implements MVPlugin, Listener {
	MultiverseTeleportFilter plugin;
//	PluginDescriptionFile pluginyml = this.getDescription();
	
	private MultiverseCore core;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[Multiverse-TeleportFilter] ";
	private static final int PROTOCOL = 1;
	private FileConfiguration config;
	
	@Override    
	public void onEnable() {
    	// Retrieves the configuration file, loads its entries for later use
    	
		this.config = plugin.getConfig();
    	
    	// Checks if the "enabled" option in config.yml is true/false and acts accordingly
		if (!config.getBoolean("options.enabled")) {
    		log(Level.INFO, "Multiverse-Teleport filter has been disabled by config.yml. Perhaps you should uninstall it instead?");
    		System.out.println(logPrefix + "disabled by config.yml.");
    		plugin.setEnabled(false);
    	}
    	
		this.core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		// Checks if Multiverse-Core is running
		if (this.core == null) {
			log(Level.WARNING, "Multiverse-Core has not been detected running.");
			System.out.println(logPrefix + "Multiverse-Core has not been detected running.");
			System.out.println(logPrefix + "Disabling Multiverse-TeleportFilter.");
			plugin.setEnabled(false);
    	}
		if (core.getProtocolVersion() != 18) {
			if (!config.getBoolean("options.ignore-core-version-check")) {
				log(Level.SEVERE, "Multiverse-Core has been updated past Multiverse-TeleportFilter, so Multiverse-TeleportFilter has been disabled. Ask the plugin author to update the plugin, or enable \"ignore-core-version\" in config.yml!");
				System.out.println(logPrefix + "Multiverse-Core has been updated past Multiverse-TeleportFilter. Ask the plugin author to update the plugin, or enable \"ignore-core-version\" in config.yml!");
				System.out.println(logPrefix + "Disabling Multiverse-TeleportFilter.");
				plugin.setEnabled(false);
			}
			else {
				System.out.println(logPrefix + "Multiverse-Core has been updated past Multiverse-TeleportFilter. Ask the plugin author to update the plugin, or enable \"ignore-core-version\" in config.yml!");
		}
		
    	System.out.println("[Multiverse-TeleportFilter] enabled.");
    	log(Level.INFO, "Multiverse-TeleportFilter has been enabled successfully.");
    	this.core.incrementPluginCount();
    	
    	getServer().getPluginManager().registerEvents(this, this);
    }
		
	@EventHandler
	public void onTP(MVTeleportEvent e) {
		
		final MVWorldManager multiverseworldmanager = core.getMVWorldManager();
		
		Player teleportee; 
		CommandSender teleporter;
		String originName, fancyTextOriginName, destinationName, fancyTextDestinationName;
		
		teleporter = e.getTeleporter();
		teleportee = e.getTeleportee();
		
		originName = teleportee.getWorld().getName();
		fancyTextOriginName = multiverseworldmanager.getMVWorld(originName).getName();
		fancyTextDestinationName = e.getDestination().getName();
		
		// Gets rid of any world name Fancy Text prefixing/coloring & stylizing gunk outputted by MVDestination.getName()
		if (fancyTextDestinationName.startsWith("º")) {
			destinationName = fancyTextDestinationName.substring(2,(fancyTextDestinationName.length() - 2));
		}
		else {
			destinationName = fancyTextDestinationName;
		}
		
		/*
		System.out.println("TPF " + originName + " to " + destinationName);
		teleportee.sendMessage("TPF " + originName + " to " + fancyTextDestinationName);
		*/
		
		// Allows the console to bypass the teleportFilter check
		if (teleporter instanceof Player) {
			if (teleportFilter(teleportee, originName, destinationName) == 1) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + ".");
				return;
			}
			if (teleportFilter(teleportee, originName, destinationName) == 2) {
				e.setCancelled(true);
				teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + " when in " + fancyTextOriginName + ".");
				return;
			}
		}
		return;
	}
	
	public int teleportFilter(Player teleportee, String originName, String destinationName) {
		// returns 0 ----> Teleportee is allowed to make the teleport
		// returns 1 ----> Teleportee's teleport is denied based on a wildcard filter (i.e. all teleports to that destination world are denied)
		// returns 2 ----> Teleportee's teleport is denied based on a specific filter (i.e. teleports from this specific origin world to the
		// destination is denied)
		
		if (config.isSet("multiverse.teleportfilter." + destinationName + ".*")) {
			if (config.getBoolean("options.ignore-permissions")) return 1;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
	        {	
	        	if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 1;
	        }
			else return 0;	
		}
		if (config.isSet("multiverse.teleportfilter." + destinationName + "." + originName)) {
			if (config.getBoolean("options.ignore-permissions")) return 1;
			if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
	        {	
	        	if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + ".*")) return 0;
				if(teleportee.hasPermission("multiverse.teleportfilter." + destinationName + "." + originName)) return 0;
				else return 1;
	        }
			else return 0;	
		}
		else return 0;
		
		/*
		if (destinationName.equals("Dragon2_nether")) t = 1;
    	if (destinationName.equals("Dragon2_the_end")) t = 1;
    	if (originName.equals("Dragon2_nether") && destinationName.equals("Dragon2")) t = 2;
    	if (originName.equals("Dragon2_nether") && destinationName.equals("Main")) t = 2;
    	if (originName.equals("Dragon2_nether") && destinationName.equals("DragonCreative")) t = 2;
    	if (originName.equals("Dragon2_nether") && destinationName.equals("Creative")) t = 2;
    	if (originName.equals("Dragon2_the_end") && destinationName.equals("Dragon2")) t = 2;
    	if (originName.equals("Dragon2_the_end") && destinationName.equals("Main")) t = 2;
    	if (originName.equals("Dragon2_the_end") && destinationName.equals("DragonCreative")) t = 2;
    	if (originName.equals("Dragon2_the_end") && destinationName.equals("Creative")) t = 2;
    	*/
	}

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
