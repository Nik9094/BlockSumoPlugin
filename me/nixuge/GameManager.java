package me.nixuge;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import me.nixuge.config.GameConfig;
import me.nixuge.config.MapConfig;
import me.nixuge.enums.GameState;
import me.nixuge.objects.BsPlayer;
import me.nixuge.objects.McMap;
import me.nixuge.runnables.BlockManagerRunnable;
import me.nixuge.runnables.GameRunnable;
import me.nixuge.runnables.ScoreboardRunnable;
import me.nixuge.utils.InventoryUtils;
import me.nixuge.utils.ScoreboardUtils;
import me.nixuge.utils.TextUtils;

public class GameManager {

    public GameManager() {
        map = new McMap(
                MapConfig.getSpawns(),
                MapConfig.getCenterBlock(),
                MapConfig.getCenterArea(),
                MapConfig.getWorld());

        blockSumo = BlockSumo.getInstance();
        setGameState(GameState.WAITING);
    }

    private GameRunnable gameRunnable;
    private BlockManagerRunnable blockDestroyRunnable;
    private ScoreboardRunnable scoreboardRunnable;

    public GameRunnable getGameRunnable() {
        return gameRunnable;
    }

    public BlockManagerRunnable getBlockDestroyRunnable() {
        return blockDestroyRunnable;
    }

    public ScoreboardRunnable getScoreboardRunnable() {
        return scoreboardRunnable;
    }

    private McMap map;
    private PlayerManager pManager = new PlayerManager();
    private GameState state = GameState.WAITING;
    private BlockSumo blockSumo;

    public McMap getMcMap() {
        return map;
    }

    public GameState getGameState() {
        return state;
    }

    public PlayerManager getPlayerMgr() {
        return pManager;
    }

    private void setGameState(GameState gameState) {
        // unregister previous ones
        for (Listener listener : state.getInstances()) {
            HandlerList.unregisterAll(listener);
        }
        state.clearInstances();

        // set new state var
        state = gameState;

        // make new instances for the new ones from the classes
        Class<?>[] classes = gameState.getListeners();
        try {
            for (Class<?> c : classes) {
                Constructor<?> cons = c.getConstructor();
                Listener listener = (Listener) cons.newInstance();
                state.addInstance(listener);
                blockSumo.getPluginManager().registerEvents(listener, blockSumo);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            // ignore all errors because why should we look at those
            e.printStackTrace();
        }
    }

    public void startGame() {
        startGame(false);
    }

    public void startGame(boolean bypass) {
        // checks
        if (pManager.getPlayers().size() < GameConfig.getMinPlayers() && !bypass) {
            Bukkit.broadcastMessage("Not enough players !");
            return;
        }
        if (state != GameState.WAITING && !bypass) {
            Bukkit.broadcastMessage("Wrong state to start a game !" + state);
            return;
        }
        TextUtils.broadcastGame("Starting!");

        setGameState(GameState.PLAYING);

        // tp players & init their inventory
        pManager.getPlayers().forEach((p) -> p.getBukkitPlayer().teleport(map.getRandomSpawn()));
        InventoryUtils.setupInventories(pManager.getPlayers());

        // set runnables
        blockDestroyRunnable = new BlockManagerRunnable();
        blockDestroyRunnable.runTaskTimer(blockSumo, 1, 1);

        gameRunnable = new GameRunnable();
        gameRunnable.runTaskTimer(blockSumo, 20, 20);

        ScoreboardUtils.resetScoreboards();
        scoreboardRunnable = new ScoreboardRunnable();
        scoreboardRunnable.runTaskTimer(blockSumo, 0, 20);
    }

    public void checkGameEnd() {
        int alivePlayerCount = 0;
        for (BsPlayer p : pManager.getPlayers()) {
            if (!p.isDead())
                alivePlayerCount++;
        }
        if (alivePlayerCount < 2)
            forceEndGame();
    }

    public void forceEndGame() {
        List<BsPlayer> winners = new ArrayList<BsPlayer>();
        for (BsPlayer p : pManager.getPlayers()) {
            if (!p.isDead())
                winners.add(p);
        }

        setGameState(GameState.DONE);
        gameRunnable.cancel();

        //TODO HERE: better end bc this sucks
        Bukkit.broadcastMessage("GAME DONE PLAYING ! WINNER(s):");
        for (BsPlayer p : winners) {
            Bukkit.broadcastMessage(p.getBukkitPlayer().getName());
        }
    }
}
