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
            plugin.getLogger().log(Level.INFO, config.getString("message_file_updates"));
            save();
        }
    }
}
