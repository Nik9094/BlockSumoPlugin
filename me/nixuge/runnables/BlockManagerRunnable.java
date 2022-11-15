package me.nixuge.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import me.nixuge.BlockSumo;
import me.nixuge.enums.Color;
import me.nixuge.objects.BsPlayer;
import me.nixuge.objects.ExpiringBlock;
import me.nixuge.utils.PacketUtils;

public class BlockManagerRunnable extends BukkitRunnable {
    private int tick_time = 0;
    private List<ExpiringBlock> blocks = new ArrayList<>();
    List<ExpiringBlock> toRemove;

    @Override
    public void run() {
        tick_time++;
        toRemove = new ArrayList<>();

        for (ExpiringBlock block : blocks) {
            loopBlockStates(block);
            loopBlockColors(block);
        }
        for (ExpiringBlock block : toRemove) {
            blocks.remove(block); // remove after to avoid causing issues in the loop
        }
    }

    @SuppressWarnings("deprecation")
    private void loopBlockColors(ExpiringBlock block) {
        int[] colors = block.getColorChanges();
        int length = colors.length;
        for (int i = 0; i < length; i++) {
            if (colors[i] == tick_time) {
                Location loc = block.asLocation();
                if (i+1 == length) { //if last element
                    //NOTE: will see if I keep it like that
                    //or just remove the lastColor altogether and do it just
                    //like bwpractice rn. For now keeping it.
                    loc.getBlock().setData(block.getLastColor().getByteColor());
                } else {
                    loc.getBlock().setData(Color.getRandomColor().getByteColor());
                }
            }
        }
    }

    private void loopBlockStates(ExpiringBlock block) {
        int[] states = block.getStates();
        for (int i = 0; i < states.length; i++) {
            if (states[i] == tick_time) {
                if (i < 10) {
                    sendBreakBlockPacket(block.asLocation(), i, block.getBreakerId());
                } else {
                    breakBlockParticles(block.asLocation());
                    sendBreakBlockPacket(block.asLocation(), i, block.getBreakerId()); // reset state
                    toRemove.add(block);
                }
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void breakBlockParticles(Location loc) {
        Block block = loc.getBlock();
        int id = block.getTypeId(); //=Material.WOOL.getId() in normal circumstances
        byte data = block.getData();

        block.setType(Material.AIR);

        //See the "PacketUtils" class for more info about fields
        //not sure if I add .5f to the Y too, looks good rn
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
            EnumParticle.BLOCK_CRACK, true, 
            (float)loc.getX() + .5f, (float)loc.getY(), (float)loc.getZ() + .5f,
            0, 0, 0, 1, 10, 
            new int[] { id | (data << 12) });

        PacketUtils.sendPacketAllPlayers(packet);
    }

    private void sendBreakBlockPacket(Location loc, int stage, int breakerId) {
        // -> see https://www.spigotmc.org/threads/block-break-state.266966/
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        // int = id of player who is hitting
        // set it to a random one, everytime the same tho so that it doesn't look weird
        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(
                breakerId, new BlockPosition(x, y, z), stage);

        int dimension;

        List<BsPlayer> gamePlayers = BlockSumo.getInstance().getGameMgr()
                .getPlayerMgr().getPlayers();

        for (BsPlayer bsPlayer : gamePlayers) {
            if (!bsPlayer.isLoggedOn())
                return;
            
            Player player = bsPlayer.getBukkitPlayer();

            dimension = ((CraftWorld) player.getWorld()).getHandle().dimension;

            ((CraftServer) player.getServer()).getHandle().sendPacketNearby(
                    x, y, z, 120, dimension, packet);
        }
    }

    public int getTickTime() {
        return tick_time;
    }

    public void addBlock(ExpiringBlock block) {
        blocks.add(block);
    }

    public void removeBlock(Location location) {
        for (ExpiringBlock b : blocks) {
            if (location.equals(b.asLocation())) {
                sendBreakBlockPacket(b.asLocation(), 10, b.getBreakerId()); // reset state
                blocks.remove(b);
                break; // should only ever be 1 at the same place so breaking is fine
            }
        }
    }
}