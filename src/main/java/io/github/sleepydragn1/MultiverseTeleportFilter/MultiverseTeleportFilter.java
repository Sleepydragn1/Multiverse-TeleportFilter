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

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;
import com.onarandombox.MultiverseCore.destination.WorldDestination;

public final class MultiverseTeleportFilter extends JavaPlugin implements MVPlugin, Listener {
	MultiverseTeleportFilter plugin;
	PluginDescriptionFile pluginyml = this.getDescription();
	
	private MultiverseCore core;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[Multiverse-TeleportFilter] ";
	
	@Override    
	public void onEnable() {
    	// Retrieves the configuration file, loads its entries for later use
    	/*plugin.getConfig();
    	if (plugin.getConfig().getBoolean("options.enabled")) {
    		log.info(logPrefix + "Multiverse-Teleport filter has been disabled by config.yml. Perhaps you should uninstall it instead?");
    		System.out.println("[Multiverse-TeleportFilter] disabled by config.yml.");
    		plugin.setEnabled(false);
    	}
    	*/
		this.core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		if (this.core == null) {
			log.warning(logPrefix + "Multiverse-Core has not been detected running.");
			System.out.println("[Multiverse-TeleportFilter] Multiverse-Core has not been detected running. Disabling Multiverse-TeleportFilter.");
			plugin.setEnabled(false);
    	}
		
    	System.out.println("[Multiverse-TeleportFilter] enabled.");
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
		System.out.println("TPF " + originName + " to " + destinationName);
		teleportee.sendMessage("TPF " + originName + " to " + fancyTextDestinationName);
		
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
		// t = 0 ----> Teleportee is allowed to make the teleport
		// t = 1 ----> Teleportee's teleport is denied based on a wildcard filter (i.e. all teleports to that destination world are denied)
		// t = 2 ----> Teleportee's teleport is denied based on a specific filter (i.e. teleporst from this specific origin world to the
		// destination is denied)
		int t = 0;
		
		if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
        {	
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
        }
        return t;
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
		return 1;
	}

	@Override
	public void setCore(MultiverseCore core) {
		this.core = core;
	}
}
