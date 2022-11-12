package me.nixuge.utils;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.nixuge.BlockSumo;
import me.nixuge.enums.PlayerState;
import me.nixuge.runnables.BlockDestroyRunnable;

public class BsPlayer {
    private Player player;
    private PlayerState state;

    public BsPlayer(Player player, PlayerState pState) {
        this.player = player;
        this.state = pState;
    }

    public BsPlayer(Player player) {
        this.player = player;
        this.state = PlayerState.LOGGED_ON;
    }

    public void setBukkitPlayer(Player player) {
        //Bukkit player object gets changed on relog
        //so need this
        this.player = player;
    }

    public Player getBukkitPlayer() {
        return player;
    }

    public PlayerState getPlayerState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state;
    }

    public void addBlock(Block block) {
        BlockDestroyRunnable bdr = BlockSumo.getInstance().getGameManager().getBlockDestroyRunnable();
        if (bdr == null) {
            Bukkit.broadcastMessage("This shouldn't happen! avoakn");
            return;
        }

        bdr.addBlock(new ExpiringBlock(bdr.getTickTime(), block.getLocation()));
    }
}
