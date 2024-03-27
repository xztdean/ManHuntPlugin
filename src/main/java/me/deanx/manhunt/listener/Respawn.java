package me.deanx.manhunt.listener;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class Respawn implements Listener {
    private final ManhuntGame game;

    public Respawn(ManHuntPlugin plugin) {
        this.game = ManhuntGame.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (game.isHunter(player)) {
            player.getInventory().setItem(8, new ItemStack(Material.COMPASS));
        }
    }
}
