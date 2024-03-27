package me.deanx.manhunt.config;

import me.deanx.manhunt.ManHuntPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import java.util.logging.Level;

public class Messages {
    private static final String FILENAME = "message.yml";
    private static Messages instance = null;

    public static void initialize(ManHuntPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("Messages has already been initialized");
        }
        instance = new Messages(plugin);
    }

    public static Messages getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Messages has not been initialized");
        }
        return instance;
    }

    private final ManHuntPlugin plugin;
    private final File messagesFile;
    private final FileConfiguration config;

    private Messages(ManHuntPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), FILENAME);
        if (!messagesFile.exists()) {
            plugin.saveResource(FILENAME, false);
        }

        this.config = YamlConfiguration.loadConfiguration(messagesFile);
        checkUpdate();
    }

    private void save() {
        try {
            config.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages file", e);
        }
    }

    private void checkUpdate() {
        InputStream defaultFileStream = plugin.getResource(FILENAME);
        assert defaultFileStream != null;
        Reader reader = new InputStreamReader(defaultFileStream);
        FileConfiguration defaultsConfigs = YamlConfiguration.loadConfiguration(reader);

        boolean updated = false;
        Set<String> defaultKeys = defaultsConfigs.getKeys(true);
        for (String key : defaultKeys) {
            if (!config.contains(key, true)) {
                config.set(key, defaultsConfigs.get(key));
                updated = true;
            }
        }

        if (updated) {
            plugin.getLogger().log(Level.INFO, get("message_file_updates"));
            save();
        }
    }

    private String get(String key) {
        String message = config.getString(key);
        if (message == null) {
            plugin.getLogger().log(Level.SEVERE, "Message key not found: " + key);
            throw new IllegalArgumentException("Message key not found: " + key);
        }
        return message;
    }

    public String getRunnerStartMsg() {
        return get("runner_start");
    }

    public String getHunterStartMsg(int time) {
        return get("hunter_start").replace("$sec$", String.valueOf(time));
    }

    public String getHuntingStartMsgForRunner() {
        return get("runner_hunting_start");
    }

    public String getSetRunnerMsg(String playerName) {
        if (playerName == null) {
            throw new IllegalArgumentException("Player name cannot be null");
        }
        return get("string.set_runner").replace("$p$", playerName);
    }

    public String getPlayerNotFoundMsg(String playerName) {
        if (playerName == null) {
            throw new IllegalArgumentException("Player name cannot be null");
        }
        return get("string.player_not_found").replace("$p$", playerName);
    }

    public String getWinMsg() {
        return get("win");
    }

    public String getLoseMsg() {
        return get("lose");
    }

    public String getRunnerDeathMsg() {
        return get("runner_dead");
    }

    public String getRunnerEnterNetherMsg() {
        return get("runner_enter_nether");
    }

    public String getCompassSwapMsg() {
        return get("compass_swap");
    }

    public String getPortalCreatedMsg(int x, int y, int z) {
        return get("portal_created")
                .replace("$x$", String.valueOf(x))
                .replace("$y$", String.valueOf(y))
                .replace("$z$", String.valueOf(z));
    }

    public String getChangeRunnerInGameErrorMsg() {
        return get("err.change_runner_in_game");
    }

    public String getRunnerNotSetErrorMsg() {
        return get("err.runner_not_set");
    }

    public String getGameInProgressErrorMsg() {
        return get("err.game_in_progress");
    }

    public String getAddCompassErrorMsg() {
        return get("err.compass_add");
    }

    public String getNotEnoughPlayerErrorMsg() {
        return get("err.not_enough_player");
    }
}
