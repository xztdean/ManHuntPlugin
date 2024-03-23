package me.deanx.manhunt.config;

import me.deanx.manhunt.ManHuntPlugin;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Configs {
    private static Configs instance = null;

    public static void initialize(ManHuntPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("Configs has already been initialized");
        }
        instance = new Configs(plugin);
    }

    public static Configs getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Configs has not been initialized");
        }
        return instance;
    }

    private final ManHuntPlugin plugin;

    private Configs(ManHuntPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public PlayerInventory getRunnerInventory() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.runner");
        assert config != null;
        return new PlayerInventory(config);
    }

    public PlayerInventory getHunterInventory() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("config.hunter");
        assert config != null;
        return new PlayerInventory(config);
    }

    public int getStartWaitingTime() {
        return plugin.getConfig().getInt("config.hunter.waiting_time");
    }

    public GameMode getGameMode() {
        String gameModeName = plugin.getConfig().getString("config.game.gamemode");
        if (gameModeName == null) {
            return GameMode.SURVIVAL;
        }
        switch (gameModeName.toUpperCase()) {
            case "CREATIVE":
                return GameMode.CREATIVE;
            case "ADVENTURE":
                return GameMode.ADVENTURE;
            case "SPECTATOR":
                return GameMode.SPECTATOR;
            case "SURVIVAL":
            default:
                return GameMode.SURVIVAL;
        }
    }

    public Difficulty getDifficulty() {
        String difficultyName = plugin.getConfig().getString("config.environment.difficulty");
        if (difficultyName == null) {
            return Difficulty.HARD;
        }
        switch (difficultyName.toUpperCase()) {
            case "PEACEFUL":
                return Difficulty.PEACEFUL;
            case "EASY":
                return Difficulty.EASY;
            case "NORMAL":
                return Difficulty.NORMAL;
            case "HARD":
            default:
                return Difficulty.HARD;
        }
    }

    public long getWorldTime() {
        return plugin.getConfig().getLong("config.environment.time");
    }

    public int getMaxHealth() {
        return plugin.getConfig().getInt("config.game.max_health");
    }

    public boolean isCompassEnabled() {
        return plugin.getConfig().getBoolean("config.game.enable_compass");
    }

    public boolean isClearAdvancements() {
        return plugin.getConfig().getBoolean("config.game.clear_advancements");
    }

    public static class PlayerInventory {
        private final Material helmet;
        private final Material chestplate;
        private final Material leggings;
        private final Material boots;
        private final List<ItemStack> items;

        public PlayerInventory(String helmet, String chestplate, String leggings, String boots, List<String> items) {
            this.helmet = getMaterial(helmet);
            this.chestplate = getMaterial(chestplate);
            this.leggings = getMaterial(leggings);
            this.boots = getMaterial(boots);
            this.items = new ArrayList<>(items.size());
            for (String item : items) {
                String[] info = item.split(" ");
                Material material = getMaterial(info[0]);
                if (material == null) {
                    continue;
                }
                int itemNum = 1;
                if (info.length > 1) {
                    try {
                        itemNum = Integer.parseInt(info[1]);
                    } catch (NumberFormatException ignored) { }
                }
                this.items.add(new ItemStack(material, itemNum));
            }
        }

        public PlayerInventory(ConfigurationSection config) {
            this(
                config.getString("helmet"),
                config.getString("chestplate"),
                config.getString("leggings"),
                config.getString("boots"),
                config.getStringList("items"));
        }

        private Material getMaterial(String materialName) {
            if (materialName == null) {
                return null;
            }
            return Material.matchMaterial(materialName);
        }

        public Material getHelmet() {
            return helmet;
        }

        public Material getChestplate() {
            return chestplate;
        }

        public Material getLeggings() {
            return leggings;
        }

        public Material getBoots() {
            return boots;
        }

        public List<ItemStack> getItems() {
            return items;
        }
    }
}

