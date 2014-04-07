package io.github.sleepydragn1.MultiverseTeleportFilter;

import com.onarandombox.MultiverseCore.event.MVTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;

public final class MultiverseTeleportFilter extends JavaPlugin implements Listener {
    @Override    
	public void onEnable() {
    	System.out.println("[Multiverse-TeleportFilter 1.0] enabled.");
    }
		
	@EventHandler
	public void onTP(MVTeleportEvent event) {
        System.out.println("this is an event, lolololol");
	}
}
