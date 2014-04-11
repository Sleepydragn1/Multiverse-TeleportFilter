package io.github.sleepydragn1.MultiverseTeleportFilter;

import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.player;
import org.bukkit.Location;
import org.bukkit.permissions.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements Listener {
	MultiverseTeleportFilter plugin;
	
	@Override    
	public void onEnable() {
    	System.out.println("[Multiverse-TeleportFilter] enabled.");
    	
    	// Retrieves the configuration file, loads its entries for later use
    	plugin.getConfig()
    	if (plugin.getConfig().getBoolean(options.enabled)) {
    		plugin.setEnabled(false);
    		System.out.println("[Multiverse-TeleportFilter] disabled by config.yml.")
    	}
    	
    	
    }
		
	@EventHandler
	public void onTP(MVTeleportEvent event) {
		Player teleportee; 
		World origin;
		MVDestination destination;
		
		event.getTeleportee() = teleportee;
		teleportee.getWorld() = origin;
		event.getDestination() = destination;
		
		if (teleportFilter(teleportee, origin, destination)) event.cancel();
        
		//System.out.println("this is an event, lolololol");
	}
	public boolean teleportFilter(Player teleportee, World origin, MVDestination destination)
        if (!teleportee.hasPermission("multiverse.teleportfilter.bypass"))
        {	
        	Boolean filterFlag1 = false;
        	Boolean filterFlag2 = false;
        
        	if (destinationName.equals("Dragon2_nether")) filterFlag1 = true;
        	if (destinationName.equals("Dragon2_the_end")) filterFlag1 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && destinationName.equals("Dragon2")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && destinationName.equals("Main")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && destinationName.equals("DragonCreative")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_nether") && destinationName.equals("Creative")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && destinationName.equals("Dragon2")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && destinationName.equals("Main")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && destinationName.equals("DragonCreative")) filterFlag2 = true;
        	if (teleportee.getWorld().getName().equals("Dragon2_the_end") && destinationName.equals("Creative")) filterFlag2 = true;
        
        	if (filterFlag1 == true && sender instanceof Player)
        	{
        		this.messaging.sendMessage(teleportee, String.format("You're not allowed to teleport to " + destinationName + "."), false);
        		return;	
        	}
        	if (filterFlag2 == true && sender instanceof Player)
        	{
        		this.messaging.sendMessage(teleportee, String.format("You're not allowed to teleport to " + destinationName + " when in " + teleportee.getWorld().getName() + "."), false);
        		return;
        	}
        }
}
