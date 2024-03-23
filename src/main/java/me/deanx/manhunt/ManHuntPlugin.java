package me.deanx.manhunt;

import me.deanx.manhunt.command.ManHuntCommand;
import me.deanx.manhunt.command.ManHuntTabCompleter;
import me.deanx.manhunt.config.Configs;
import me.deanx.manhunt.listener.DropItem;
import me.deanx.manhunt.listener.Respawn;
import me.deanx.manhunt.listener.Result;
import me.deanx.manhunt.listener.RunnerLocation;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ManHuntPlugin extends JavaPlugin {
    private Player runner;
    private boolean status;
    private final List<Player> hunters = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("ManHunt plugin start");
        Configs.initialize(this);
        new ManHuntCommand(this);
        new ManHuntTabCompleter(this);
    }

    @Override
    public void onDisable() {
    }

    public Player getRunner() {
        return runner;
    }

    public void setRunner(Player runner) {
        this.runner = runner;
    }

    public boolean isHunter(Player player) {
        return hunters.contains(player);
    }

    public void addHunter(Player hunter) {
        if (!isHunter(hunter)) {
            this.hunters.add(hunter);
        }
    }

    public void removeHunter(Player hunter) {
        this.hunters.remove(hunter);
    }

    public void registerListener() {
        new Respawn(this);
        new Result(this);
        if (getConfig().getBoolean("config.game.enable_compass")) {
            new RunnerLocation(this);
            new DropItem(this);
        }
        status = true;
    }

    public void endGame() {
        runner = null;
        hunters.clear();
        HandlerList.unregisterAll(this);
        status = false;
    }

    public boolean isStart() {
        return status;
    }

    public void runnerLose() {
        runner.playSound(runner.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1);
        runner.sendTitle(getConfig().getString("string.lose"), null, 20, 40, 20);
        for (Player hunter : hunters) {
            hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            hunter.sendTitle(getConfig().getString("string.win"),
                    getConfig().getString("string.runner_dead"), 20, 40, 20);
        }
        endGame();
    }

    public void runnerWin() {
        runner.playSound(runner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        runner.sendTitle(getConfig().getString("string.win"), null, 20, 40, 20);
        for (Player hunter : hunters) {
            hunter.playSound(hunter.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1);
            hunter.sendTitle(getConfig().getString("string.lose"),
                    getConfig().getString("string.runner_enter_nether"), 20, 40, 20);
        }
        endGame();
    }

    public void updateCompass() {
        Location location = runner.getLocation();
        for (Player hunter : hunters) {
            hunter.setCompassTarget(location);
        }
    }
}
