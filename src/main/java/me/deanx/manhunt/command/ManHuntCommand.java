package me.deanx.manhunt.command;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.listener.WaitingTimeControl;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ManHuntCommand implements CommandExecutor {
    private final ManHuntPlugin plugin;

    public ManHuntCommand(ManHuntPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("ManHunt").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        String ret = "";
        if (args[0].equalsIgnoreCase("start")) {
            ret = start();
        } else if (args[0].equalsIgnoreCase("stop")) {
            plugin.endGame();
        } else if (args[0].equalsIgnoreCase("compass")) {
            if (sender instanceof Player) {
                giveCompass((Player) sender);
            }
        } else if (args[0].equalsIgnoreCase("runner")) {
            ret = labelPlayer(args[1]);
        } else if (args[0].equalsIgnoreCase("random")) {
            ret = randomPlayer();
        } else {
            return false;
        }
        if (!ret.isEmpty()) {
            sender.sendMessage(ret);
        }
        return true;
    }

    private String start() {
        if (plugin.getRunner() == null) {
            return plugin.getConfig().getString("string.err.runner_empty");
        } else if (plugin.isStart()) {
            return plugin.getConfig().getString("string.err.game_running");
        } else if (Bukkit.getOnlinePlayers().size() < 2) {
            return plugin.getConfig().getString("string.err.not_enough_player");
        }
        Player runner = plugin.getRunner();

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.environment");
        assert config != null;
        int time = config.getInt("time");
        World world = runner.getWorld();
        world.setTime(time);
        String difficulty = config.getString("difficulty");
        if (difficulty != null) {
            switch (difficulty) {
                case "HARD":
                    world.setDifficulty(Difficulty.HARD);
                    break;
                case "NORMAL":
                    world.setDifficulty(Difficulty.NORMAL);
                    break;
                case "EASY":
                    world.setDifficulty(Difficulty.EASY);
                    break;
                case "PEACEFUL":
                    world.setDifficulty(Difficulty.PEACEFUL);
            }
        }
        removeItemsOnGround(world);

        setRunner(runner);
        Player[] playerList = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (Player p : playerList) {
            if (p != runner) {
                setHunter(p);
                plugin.addHunter(p);
            }
        }
        plugin.registerListener();
        final long waitingTime = plugin.getConfig().getLong("config.hunter.waiting_time");
        new WaitingTimeControl(plugin, waitingTime * 20);
        return "";
    }

    private void setRunner(Player runner) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.runner");
        assert config != null;
        setInventory(runner, config);
        setInitialState(runner);
        runner.setBedSpawnLocation(null);
        runner.getWorld().setSpawnLocation(runner.getLocation());
        runner.sendMessage(plugin.getConfig().getString("string.runner_start_msg"));
        final int time = plugin.getConfig().getInt("config.hunter.waiting_time");
        new Thread(() -> {
            try {
                Thread.sleep(time * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runner.sendMessage(plugin.getConfig().getString("string.runner_hunting_start_msg"));
        }).start();
    }

    private void setHunter(Player hunter) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.hunter");
        assert config != null;
        setInventory(hunter, config);
        final int time = config.getInt("waiting_time");
        final int ticks = time * 20;
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, ticks, 128));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, ticks, 128));
        hunter.setBedSpawnLocation(plugin.getRunner().getLocation(), true);
        setInitialState(hunter);
        hunter.sendMessage(plugin.getConfig().getString("string.hunter_start_msg")
                           .replace("$sec$", String.valueOf(time)));
        new Thread(() -> waitingCountdown(hunter, time)).start();
    }

    private void removeItemsOnGround(World world) {
        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (entity instanceof Item) {
                entity.remove();
            }
        }
    }

    private void setInventory(Player player, ConfigurationSection config) {
        String helmetName = config.getString("helmet");
        String chestplateName = config.getString("chestplate");
        String leggingsName = config.getString("leggings");
        String bootsName = config.getString("boots");
        List<String> itemsName = config.getStringList("items");

        Material helmet = null;
        Material chestplate = null;
        Material leggings = null;
        Material boots = null;

        if (helmetName != null) {
            helmet = Material.getMaterial(helmetName);
        }
        if (chestplateName != null) {
            chestplate = Material.getMaterial(chestplateName);
        }
        if (leggingsName != null) {
            leggings = Material.getMaterial(leggingsName);
        }
        if (bootsName != null) {
            boots = Material.getMaterial(bootsName);
        }

        List<ItemStack> items = new ArrayList<>();
        player.getInventory().clear();

        for (String name : itemsName) {
            String[] info = name.split(" ");
            Material material = Material.getMaterial(info[0]);
            if (material == Material.COMPASS) {
                giveCompass(player);
            } else if (material != null) {
                int num = 1;
                if (info.length > 1) {
                    num = Integer.parseInt(info[1]);
                }
                items.add(new ItemStack(material, num));
            } else {
                System.err.println("[ManHunt] Config for Player's item contains error.");
            }
        }

        if (helmet != null) {
            player.getInventory().setHelmet(new ItemStack(helmet));
        }
        if (chestplate != null) {
            player.getInventory().setChestplate(new ItemStack(chestplate));
        }
        if (leggings != null) {
            player.getInventory().setLeggings(new ItemStack(leggings));
        }
        if (boots != null) {
            player.getInventory().setBoots(new ItemStack(boots));
        }
        for (ItemStack item : items) {
            player.getInventory().addItem(item);
        }
    }

    private void waitingCountdown(Player player, int time) {
        for (int i = time; i > 0; i--) {
            player.sendTitle(String.valueOf(i), "", 2, 16, 2);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        player.sendTitle("Go!", "", 2, 16, 2);
    }

    private void giveCompass(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(Material.COMPASS)) {
            int index = inventory.first(Material.COMPASS);
            if (index != 8) {
                ItemStack itemToSwap = inventory.getItem(8);
                inventory.setItem(8, inventory.getItem(index));
                inventory.setItem(index, itemToSwap);
                player.sendMessage(plugin.getConfig().getString("string.compass_swap"));
            }
        } else {
            if (inventory.firstEmpty() == -1) {
                player.sendMessage(plugin.getConfig().getString("string.err.compass_add"));
                return;
            }
            ItemStack itemIn8 = inventory.getItem(8);
            inventory.setItem(8, new ItemStack(Material.COMPASS));
            if (itemIn8 != null) {
                inventory.addItem(itemIn8);
            }
            if (plugin.isStart()) {
                player.setCompassTarget(plugin.getRunner().getLocation());
            }
        }
    }

    private void setInitialState(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.game");
        assert config != null;
        double maxHealth = config.getDouble("max_health");
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(5);

        String gamemode = config.getString("gamemode");
        if (gamemode != null) {
            switch (gamemode) {
                case "SURVIVAL":
                    player.setGameMode(GameMode.SURVIVAL);
                    break;
                case "ADVENTURE":
                    player.setGameMode(GameMode.ADVENTURE);
                    break;
                case "CREATIVE":
                    player.setGameMode(GameMode.CREATIVE);
            }
        }

        // reset advancement
        if (config.getBoolean("clear_advancement")) {
            for (Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator(); iterator.hasNext(); ) {
                AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
                for (String criteria : progress.getAwardedCriteria())
                    progress.revokeCriteria(criteria);
            }
        }

    }

    private String labelPlayer(String name) {
        if (plugin.isStart()) {
            return plugin.getConfig().getString("string.err.change_runner_in_game");
        }
        Player p = Bukkit.getPlayer(name);
        if (p != null) {
            labelRunner(p);
            return "";
        } else {
            return plugin.getConfig().getString("string.cannot_find_player").replace("$p$", name);
        }
    }

    private void labelRunner(Player p) {
        plugin.setRunner(p);
        Bukkit.getServer().broadcastMessage(plugin.getConfig().getString("string.set_runner")
                                            .replace("$p$", p.getDisplayName()));
    }

    private String randomPlayer() {
        if (plugin.isStart()) {
            return plugin.getConfig().getString("string.err.change_runner_in_game");
        }
        Player[] playerList = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        int random = new Random().nextInt(playerList.length);
        Player selectedPlayer = playerList[random];
        selectionAnimation(selectedPlayer);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            labelRunner(selectedPlayer);
        }, 60);
        return "";
    }

    private void selectionAnimation(Player selected) {
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
}
