package me.deanx.manhunt.command;

import me.deanx.manhunt.ManHuntPlugin;
import me.deanx.manhunt.interfaces.CompassNBT;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManHunt implements CommandExecutor {
    private final ManHuntPlugin plugin;

    public ManHunt(ManHuntPlugin plugin) {
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
            return "Please set the runner first";
        } else if (plugin.isStart()) {
            return "Hunting Game is already running.";
        }
        Player runner = plugin.getRunner();
        runner.getWorld().setTime(1000);
        runner.getWorld().setDifficulty(Difficulty.HARD);
        setRunner(runner);
        Player[] playerList = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (Player p : playerList) {
            if (p != runner) {
                setHunter(p);
                plugin.addHunter(p);
            }
        }
        plugin.registerListener();
        return "";
    }

    private void setRunner(Player runner) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.runner");
        assert config != null;
        setInventory(runner, config);
        setInitialState(runner);
        runner.sendMessage("You are Runner. RUN!");
        final int time = plugin.getConfig().getInt("config.hunter.waitting_time");
        new Thread(() -> {
            try {
                Thread.sleep(time * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runner.sendMessage("Hunters start hunting now.");
        }).start();
    }

    private void setHunter(Player hunter) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.hunter");
        assert config != null;
        setInventory(hunter, config);
        final int time = config.getInt("waitting_time");
        final int ticks = time * 20;
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, ticks, 128));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 128));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, 129)); // 129 for -128
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, ticks, 128));
        hunter.setBedSpawnLocation(plugin.getRunner().getLocation(), true);
        setInitialState(hunter);
        hunter.sendMessage("You are hunter, please wait for " + time + "s.");
        new Thread(() -> waitingCountdown(hunter, time)).start();
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
        for (String name : itemsName) {
            String[] info = name.split(" ");
            Material material = Material.getMaterial(info[0]);
            if (material != null) {
                int num = 1;
                if (info.length > 1) {
                    num = Integer.parseInt(info[1]);
                }
                items.add(new ItemStack(material, num));
            } else {
                System.err.println("[ManHunt] Config for Player's item contains error.");
            }
        }

        player.getInventory().clear();
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
        player.getInventory().addItem(new ItemStack(Material.COMPASS));
        if (plugin.isStart()) {
            CompassNBT.getInstance().updateInventory(player);
        }
    }

    private void setInitialState(Player player) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setGameMode(GameMode.SURVIVAL);
        // reset advancement
        for(Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator(); iterator.hasNext();) {
            AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
            for (String criteria : progress.getAwardedCriteria())
                progress.revokeCriteria(criteria);
        }

    }

    private String labelPlayer(String name) {
        if (plugin.isStart()) {
            return "Cannot change runner during game";
        }
        Player p = Bukkit.getPlayer(name);
        if (p != null) {
            plugin.setRunner(p);
            Bukkit.getServer().broadcastMessage(p.getDisplayName() + " is set to be Runner.");
            return "";
        } else {
            return "Can't find player " + name;
        }
    }
}
