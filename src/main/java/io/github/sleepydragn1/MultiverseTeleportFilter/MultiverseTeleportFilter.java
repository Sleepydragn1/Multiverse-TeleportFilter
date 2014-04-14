package io.github.sleepydragn1.MultiverseTeleportFilter;

import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;
import com.onarandombox.MultiverseCore.utils.WorldManager;
import com.onarandombox.MultiverseCore.destination.WorldDestination;

import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.*;
import org.bukkit.plugin.messaging.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.permissions.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements Listener {
	MultiverseTeleportFilter plugin;
	PluginDescriptionFile pluginyml = this.getDescription();
	
	@Override    
	public void onEnable() {
    	System.out.println("[Multiverse-TeleportFilter] enabled - Version " + pluginyml.getVersion() + ".");
    	
    	// Retrieves the configuration file, loads its entries for later use
    	/*plugin.getConfig();
    	if (plugin.getConfig().getBoolean("options.enabled")) {
    		plugin.setEnabled(false);
    		System.out.println("[Multiverse-TeleportFilter] disabled by config.yml.");
    	}
    	*/
    	if (!getServer().getPluginManager().isPluginEnabled("MultiverseCore")) {
    		plugin.setEnabled(false);
    		System.out.println("[Multiverse-TeleportFilter] Multiverse-Core has not been detected running. Disabling Multiverse-TeleportFilter.");
    	}
    	
    	getServer().getPluginManager().registerEvents(this, this);
    }
		
	@EventHandler
	public void onTP(MVTeleportEvent e) {
		
		Player teleportee; 
		String originName, fancyTextDestinationName, destinationName;
		
		teleportee = e.getTeleportee();
		originName = teleportee.getWorld().getName();
		fancyTextDestinationName = e.getDestination().getName();
		destinationName = getFancyText(fancyTextDestinationName);
		//destinationName = fancyTextDestinationName.substring(1,(fancyTextDestinationName.length() - 1));
		
		System.out.println("TPF " + originName + " to " + destinationName);
		teleportee.sendMessage("TPF " + originName + " to " + fancyTextDestinationName);
		
		if (teleportFilter(teleportee, originName, destinationName) == 1) {
			e.setCancelled(true);
			teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + ".");
		}
		if (teleportFilter(teleportee, originName, destinationName) == 2) {
			e.setCancelled(true);
			teleportee.sendMessage("You're not allowed to teleport to " + fancyTextDestinationName + " when in " + originName + ".");
		}
	}
	
	public int teleportFilter(Player teleportee, String originName, String destinationName) {
		int i = 0;
		
		if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
        {	
        	if (destinationName.equals("Dragon2_nether")) i = 1;
        	if (destinationName.equals("Dragon2_the_end")) i = 1;
        	if (originName.equals("Dragon2_nether") && destinationName.equals("Dragon2")) i = 2;
        	if (originName.equals("Dragon2_nether") && destinationName.equals("Main")) i = 2;
        	if (originName.equals("Dragon2_nether") && destinationName.equals("DragonCreative")) i = 2;
        	if (originName.equals("Dragon2_nether") && destinationName.equals("Creative")) i = 2;
        	if (originName.equals("Dragon2_the_end") && destinationName.equals("Dragon2")) i = 2;
        	if (originName.equals("Dragon2_the_end") && destinationName.equals("Main")) i = 2;
        	if (originName.equals("Dragon2_the_end") && destinationName.equals("DragonCreative")) i = 2;
        	if (originName.equals("Dragon2_the_end") && destinationName.equals("Creative")) i = 2;
        }
        return i;
	}
}
