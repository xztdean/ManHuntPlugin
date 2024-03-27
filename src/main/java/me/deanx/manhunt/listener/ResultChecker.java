package me.deanx.manhunt.listener;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class ResultChecker implements Listener {
    private final ManhuntGame game;

    public ResultChecker(ManHuntPlugin plugin) {
        this.game = ManhuntGame.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (game.isRunner(player)) {
            game.runnerLose();
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (game.isRunner(player)) {
            game.runnerWin();
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        Entity creatorEntity = event.getEntity();
        if (creatorEntity instanceof Player) {
            Player creator = (Player) creatorEntity;
            if (game.isRunner(creator) && event.getReason() == PortalCreateEvent.CreateReason.FIRE) {
                ManhuntGame.getInstance().notifyHunterPortalCreate(event.getBlocks().get(0).getLocation());
            }
        }
    }
}
