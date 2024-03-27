package me.deanx.manhunt.listener;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WaitingTimeControl implements Listener {
    private final ManhuntGame game;

    public WaitingTimeControl(ManHuntPlugin plugin) {
        this.game = ManhuntGame.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public WaitingTimeControl(ManHuntPlugin plugin, long ticks) {
        this(plugin);
        Bukkit.getScheduler().runTaskLater(plugin, this::unregister, ticks);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (game.isHunter(player)) {
                event.setCancelled(true);
            } else {
                // Only damage from player to runner will be cancelled.
                Entity damager = event.getDamager();
                if (damager instanceof Player) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMoving(PlayerMoveEvent event) {
        if (game.isHunter(event.getPlayer())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            assert to != null;
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (game.isHunter(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        if (game.isHunter(event.getPlayer()) && event.getReason().contains("Flying is not enabled")) {
            event.setCancelled(true);
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
