package me.deanx.manhunt;

import me.deanx.manhunt.config.Configs;
import me.deanx.manhunt.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ManhuntGame {
    private static ManhuntGame instance = null;

    public static ManhuntGame getInstance() {
        if (instance == null) {
            instance = new ManhuntGame();
        }
        return instance;
    }

    private Player runner = null;
    private boolean isRunning = false;
    private final List<Player> hunters = new ArrayList<>();


    public void startGame() {
        if (isRunning) {
            throw new IllegalStateException(Messages.getInstance().getGameInProgressErrorMsg());
        } else if (runner == null) {
            throw new IllegalStateException(Messages.getInstance().getRunnerNotSetErrorMsg());
        } else if (Bukkit.getOnlinePlayers().size() < 2) {
            throw new IllegalStateException(Messages.getInstance().getNotEnoughPlayerErrorMsg());
        }

        prepareWorld();
    }

    private void prepareWorld() {
        Configs configs = Configs.getInstance();

        World world = runner.getWorld();
        world.setFullTime(configs.getWorldTime());
        world.setDifficulty(configs.getDifficulty());
        world.getEntitiesByClass(Item.class).forEach(Item::remove);
    }
}
