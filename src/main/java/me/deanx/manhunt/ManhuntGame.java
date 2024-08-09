package me.deanx.manhunt;

import me.deanx.manhunt.config.Configs;
import me.deanx.manhunt.config.Messages;
import me.deanx.manhunt.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ManhuntGame {
    private static ManhuntGame instance = null;

    public static void initialize(ManHuntPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("ManhuntGame has already been initialized");
        }
        instance = new ManhuntGame(plugin);
    }

    public static ManhuntGame getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ManhuntGame has not been initialized");
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

        generateHunterList();

        prepareWorld();
        prepareRunner();
        prepareHunters();

        registerListener();
        isRunning = true;
    }

    public void stopGame() {
        Bukkit.getScheduler().cancelTasks(plugin);
        HandlerList.unregisterAll(plugin);
        isRunning = false;

        instance = null;
        initialize(plugin);
    }

    public void setRunner(Player runner) {
        if (isRunning) {
            throw new IllegalStateException(Messages.getInstance().getChangeRunnerInGameErrorMsg());
        }
        this.runner = runner;
        Bukkit.getServer().broadcastMessage(Messages.getInstance().getSetRunnerMsg(runner.getDisplayName()));
    }

    public void randomSelectRunner() {
        if (isRunning) {
            throw new IllegalStateException(Messages.getInstance().getChangeRunnerInGameErrorMsg());
        }
        Player[] playerList = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        int random = new Random().nextInt(playerList.length);
        Player selectedPlayer = playerList[random];
        TitleDisplay.getInstance().displayRandomPlayerSelect(selectedPlayer);
        Bukkit.getScheduler().runTaskLater(plugin, () -> setRunner(selectedPlayer), 60); // Display animation for 60 ticks
    }

    public boolean isRunner(Player player) {
        return runner == player;
    }

    public boolean isHunter(Player player) {
        return hunters.contains(player);
    }

    public void notifyHunterPortalCreate(Location portalLocation) {
        for (Player hunter : hunters) {
            hunter.sendMessage(Messages.getInstance().getPortalCreatedMsg(portalLocation.getBlockX(), portalLocation.getBlockY(), portalLocation.getBlockZ()));
        }
        runner.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
    }

    private void generateHunterList() {
        hunters.addAll(Bukkit.getOnlinePlayers());
        hunters.remove(runner);
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
        runner.sendMessage(Messages.getInstance().getRunnerStartMsg());

        double tickRate = runner.getServer().getServerTickManager().getTickRate();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runner.sendMessage(Messages.getInstance().getHuntingStartMsgForRunner());
        }, (long) (configs.getStartWaitingTime() * tickRate));
    }

    private void prepareHunters() {
        Configs configs = Configs.getInstance();
        double tickRate = runner.getServer().getServerTickManager().getTickRate();
        int startWaitingTime = configs.getStartWaitingTime();
        int waitingTicks = (int) (startWaitingTime * tickRate);
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.BLINDNESS, waitingTicks, 0),
            new PotionEffect(PotionEffectType.SLOW_DIGGING, waitingTicks, 128),
            new PotionEffect(PotionEffectType.HEAL, waitingTicks, 128)
        );

        for (Player hunter : hunters) {
            setPlayerInventory(hunter, configs.getHunterInventory());
            setInitialState(hunter);
            hunter.addPotionEffects(effects);
            hunter.sendMessage(Messages.getInstance().getHunterStartMsg(startWaitingTime));
        }

        TitleDisplay.getInstance().countdown(startWaitingTime, hunters, "Go!");
        new WaitingTimeControl(plugin, waitingTicks);
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
        player.setRespawnLocation(null);

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

    private void registerListener() {
        new Respawn(plugin);
        new ResultChecker(plugin);
        if (Configs.getInstance().isCompassEnabled()) {
            new RunnerLocation(plugin);
            new DropItem(plugin);
        }
    }

    public void updateCompass() {
        Location location = runner.getLocation();
        for (Player hunter : hunters) {
            hunter.setCompassTarget(location);
        }
    }

    public void runnerWin() {
        Messages messages = Messages.getInstance();
        runner.playSound(runner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        runner.sendTitle(messages.getWinMsg(), null, 20, 40, 20);
        for (Player hunter : hunters) {
            hunter.playSound(hunter.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1);
            hunter.sendTitle(messages.getLoseMsg(), messages.getRunnerEnterNetherMsg(), 20, 40, 20);
        }
        stopGame();
    }

    public void runnerLose() {
        Messages messages = Messages.getInstance();
        runner.playSound(runner.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1);
        runner.sendTitle(messages.getLoseMsg(), null, 20, 40, 20);
        for (Player hunter : hunters) {
            hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            hunter.sendTitle(messages.getWinMsg(), messages.getRunnerDeathMsg(), 20, 40, 20);
        }
        stopGame();
    }
}
