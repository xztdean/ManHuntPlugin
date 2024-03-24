package me.deanx.manhunt;

import me.deanx.manhunt.config.Configs;
import me.deanx.manhunt.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ManhuntGame {
    private static ManhuntGame instance = null;

    public static ManhuntGame getInstance(ManHuntPlugin plugin) {
        if (instance == null) {
            instance = new ManhuntGame(plugin);
        }
        return instance;
    }

    private final ManHuntPlugin plugin;
    private Player runner = null;
    private boolean isRunning = false;
    private final List<Player> hunters = new ArrayList<>();

    public ManhuntGame(ManHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public void startGame() {
        if (isRunning) {
            throw new IllegalStateException(Messages.getInstance().getGameInProgressErrorMsg());
        } else if (runner == null) {
            throw new IllegalStateException(Messages.getInstance().getRunnerNotSetErrorMsg());
        } else if (Bukkit.getOnlinePlayers().size() < 2) {
            throw new IllegalStateException(Messages.getInstance().getNotEnoughPlayerErrorMsg());
        }

        prepareWorld();
        prepareRunner();
    }

    private void prepareWorld() {
        Configs configs = Configs.getInstance();

        World world = runner.getWorld();
        world.setFullTime(configs.getWorldTime());
        world.setDifficulty(configs.getDifficulty());
        world.getEntitiesByClass(Item.class).forEach(Item::remove);
        world.setSpawnLocation(runner.getLocation());
    }

    private void prepareRunner() {
        Configs configs = Configs.getInstance();
        setPlayerInventory(runner, configs.getRunnerInventory());
        setInitialState(runner);
        runner.setRespawnLocation(null);
        runner.sendMessage(Messages.getInstance().getRunnerStartMsg());

        double tickRate = runner.getServer().getServerTickManager().getTickRate();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runner.sendMessage(Messages.getInstance().getHuntingStartMsgForRunner());
        }, (long) (configs.getStartWaitingTime() * tickRate));
    }

    private void setPlayerInventory(Player player, Configs.PlayerInventory inventory) {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        Material helmet = inventory.getHelmet();
        Material chestplate = inventory.getChestplate();
        Material leggings = inventory.getLeggings();
        Material boots = inventory.getBoots();
        if (helmet != null) {
            playerInventory.setHelmet(new ItemStack(helmet));
        }
        if (chestplate != null) {
            playerInventory.setChestplate(new ItemStack(chestplate));
        }
        if (leggings != null) {
            playerInventory.setLeggings(new ItemStack(leggings));
        }
        if (boots != null) {
            playerInventory.setBoots(new ItemStack(boots));
        }

        List<ItemStack> items = inventory.getItems();
        boolean hasCompass = items.removeIf(item -> item.getType() == Material.COMPASS);
        if (hasCompass) { // Always place compass in the last slot
            giveCompass(player);
        }
        HashMap<Integer, ItemStack> notAdded = playerInventory.addItem(items.toArray(new ItemStack[0]));
        if (!notAdded.isEmpty()) { // Drop items that cannot be added to inventory
            notAdded.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
        }
    }

    private void setInitialState(Player player) {
        Configs configs = Configs.getInstance();

        double maxHealth = configs.getMaxHealth();
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert maxHealthAttr != null;
        maxHealthAttr.setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20); // Game default value
        player.setSaturation(5); // Game default value

        player.setGameMode(configs.getGameMode());

        // Clear all advancement
        if (configs.isClearAdvancements()) {
            Bukkit.getServer().advancementIterator().forEachRemaining(advancement -> {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                progress.getAwardedCriteria().forEach(progress::revokeCriteria);
            });
        }

    }

    public void giveCompass(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(Material.COMPASS)) {
            int index = inventory.first(Material.COMPASS);
            if (index != 8) {
                ItemStack itemToSwap = inventory.getItem(8);
                inventory.setItem(8, inventory.getItem(index));
                inventory.setItem(index, itemToSwap);
                player.sendMessage(Messages.getInstance().getCompassSwapMsg());
            }
        } else {
            if (inventory.firstEmpty() == -1) { // Inventory is full
                player.sendMessage(Messages.getInstance().getAddCompassErrorMsg());
                return;
            }
            ItemStack itemIn8 = inventory.getItem(8);
            inventory.setItem(8, new ItemStack(Material.COMPASS));
            if (itemIn8 != null) {
                inventory.addItem(itemIn8);
                player.sendMessage(Messages.getInstance().getCompassSwapMsg());
            }
            if (isRunning) {
                player.setCompassTarget(runner.getLocation());
            }
        }
    }
}
