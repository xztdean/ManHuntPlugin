package me.deanx.manhunt.listener;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RunnerLocation implements Listener {
    private final ManhuntGame game;

    public RunnerLocation(ManHuntPlugin plugin) {
        this.game = ManhuntGame.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRunnerMove(PlayerMoveEvent event) {
        if (game.isRunner(event.getPlayer())) {
            game.updateCompass();
        }
    }
}
