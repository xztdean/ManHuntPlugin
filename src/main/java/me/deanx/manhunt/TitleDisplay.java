package me.deanx.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class TitleDisplay {
    static private TitleDisplay instance;

    public static void initialize(ManHuntPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("TitleDisplay has already been initialized");
        }
        instance = new TitleDisplay(plugin);
    }

    public static TitleDisplay getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TitleDisplay has not been initialized");
        }
        return instance;
    }

    private final ManHuntPlugin plugin;

    public TitleDisplay(ManHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public void displayRandomPlayerSelect(Player selected) {
        Player[] playerList = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        Random r = new Random();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int random = r.nextInt(playerList.length);
            String name = playerList[random].getDisplayName();
            for (Player p : playerList) {
                p.sendTitle(name, null, 0, 5, 0);
            }
        }, 0, 2);
        String name = ChatColor.RED + selected.getDisplayName();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            for (Player p : playerList) {
                p.sendTitle(name, null, 0, 20, 5);
            }
        }, 60);
    }

    public void countdown(int seconds, List<Player> players, String finalMessage) {
        double tickRate = players.get(0).getServer().getServerTickManager().getTickRate();
        int fadeInOutTick = (int) (tickRate * 0.1);
        int stayTick = (int) (tickRate - fadeInOutTick * 2);
        long totalTick = fadeInOutTick * 2L + stayTick;

        BukkitScheduler scheduler = Bukkit.getScheduler();

        for (int i = 0; i < seconds; ++i) {
            String timeRemaining = String.valueOf(seconds - i);
            scheduler.runTaskLater(plugin, () -> {
                for (Player player : players) {
                    player.sendTitle(timeRemaining, null, fadeInOutTick, stayTick, fadeInOutTick);
                }
            }, i * totalTick);
        }
        scheduler.runTaskLater(plugin, () -> {
            for (Player player : players) {
                player.sendTitle(finalMessage, null, fadeInOutTick, stayTick, fadeInOutTick);
            }
        }, seconds * totalTick);
    }
}
