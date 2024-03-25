package me.deanx.manhunt.listener;

import me.deanx.manhunt.ManHuntPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class DropItem implements Listener {
    public DropItem(ManHuntPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        if (drop.getItemStack().getType() == Material.COMPASS) { // Remove outdated compass
            drop.remove();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> item.getType() == Material.COMPASS); // Remove compass on death
    }
}
