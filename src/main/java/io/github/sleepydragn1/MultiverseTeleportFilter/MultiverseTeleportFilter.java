package io.github.sleepydragn1.MultiverseTeleportFilter;

import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import com.onarandombox.MultiverseCore.api.*;
import org.bukkit.*;
import org.bukkit.plugin.messaging.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.permissions.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements Listener {
	MultiverseTeleportFilter plugin;
	
	@Override    
	public void onEnable() {
    	System.out.println("[Multiverse-TeleportFilter] enabled.");
    	
    	// Retrieves the configuration file, loads its entries for later use
    	plugin.getConfig();
    	if (plugin.getConfig().getBoolean("options.enabled")) {
    		plugin.setEnabled(false);
    		System.out.println("[Multiverse-TeleportFilter] disabled by config.yml.");
    	}
    }
		
	@EventHandler
	public void onTP(MVTeleportEvent e) {
		
		Player teleportee; 
		World origin;
		MVDestination destination;
		
		teleportee = e.getTeleportee();
		origin = teleportee.getWorld();
		destination = e.getDestination();
		
		if (teleportFilter(teleportee, origin, destination) == 1) {
			e.setCancelled(true);
			teleportee.sendMessage("You're not allowed to teleport to " + destination.getName() + ".");
		}
		if (teleportFilter(teleportee, origin, destination) == 2) {
			e.setCancelled(true);
			teleportee.sendMessage("You're not allowed to teleport to " + destination.getName() + " when in " + origin.getName() + ".");
		}
        
		//System.out.println("this is an event, lolololol");
	}
	
	public int teleportFilter(Player teleportee, World origin, MVDestination destination) {
		int i = 0;
		
		if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
        {	
        	if (origin.getName().equals("Dragon2_nether")) i = 1;
        	if (origin.getName().equals("Dragon2_the_end")) i = 1;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && origin.getName().equals("Dragon2")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && origin.getName().equals("Main")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && origin.getName().equals("DragonCreative")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && origin.getName().equals("Creative")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && origin.getName().equals("Dragon2")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && origin.getName().equals("Main")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && origin.getName().equals("DragonCreative")) i = 2;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && origin.getName().equals("Creative")) i = 2;
        }
        return i;
	}
}
