package com.vanillage.ssaspawnerantiesp.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.antixray.SpawnerChunkPacketBlockController;
import com.vanillage.ssaspawnerantiesp.data.ChunkBlocks;
import com.vanillage.ssaspawnerantiesp.data.LongWrapper;
import com.vanillage.ssaspawnerantiesp.data.PlayerData;
import com.vanillage.ssaspawnerantiesp.data.Result;
import com.vanillage.ssaspawnerantiesp.data.VectorialLocation;
import com.vanillage.ssaspawnerantiesp.nms.NmsCompat;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockUpdateTask implements Consumer<ScheduledTask> {
    private final SSASpawnerAntiESP plugin;
    private final Player player;

    public BlockUpdateTask(SSASpawnerAntiESP plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void accept(ScheduledTask task) {
        update(player);
    }

    public static void hideNearbySpawners(SSASpawnerAntiESP plugin, Player player) {
        World world = player.getWorld();

        if (!plugin.isEnabled(world)) {
            return;
        }

        Location eye = player.getEyeLocation();
        List<BlockPos> targets = plugin.getSpawnerIndex().queryNear(
            world,
            eye.getX(),
            eye.getY(),
            eye.getZ(),
            plugin.getRayTraceDistance(world)
        );

        if (targets.isEmpty()) {
            return;
        }

        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Environment environment = world.getEnvironment();
        List<Packet<?>> packets = new ArrayList<>();

        for (BlockPos block : targets) {
            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }

            if (!serverLevel.getBlockState(block).is(Blocks.SPAWNER)) {
                continue;
            }

            packets.add(new ClientboundBlockUpdatePacket(block, SpawnerChunkPacketBlockController.decoyState(environment, block.getY())));
        }

        sendPackets(player, packets);
    }

    public void update(Player player) {
        PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (!plugin.validatePlayerData(player, playerData, "update")) {
            return;
        }

        VectorialLocation eye = playerData.getLocations()[0];
        World world = eye.getWorld();

        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        ConcurrentMap<LongWrapper, ChunkBlocks> chunks = playerData.getChunks();
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Environment environment = world.getEnvironment();
        Queue<Result> results = playerData.getResults();
        Result result;
        List<Packet<?>> packetsToSend = new ArrayList<>();

        while ((result = results.poll()) != null) {
            ChunkBlocks chunkBlocks = result.getChunkBlocks();

            if (chunkBlocks.getChunk() == null || chunks.get(chunkBlocks.getKey()) != chunkBlocks) {
                continue;
            }

            BlockPos block = result.getBlock();

            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }

            BlockState serverState = serverLevel.getBlockState(block);
            BlockState blockState;
            BlockEntity blockEntity = null;

            if (result.isVisible()) {
                if (serverState.getBlock() != Blocks.SPAWNER) {
                    continue;
                }

                blockState = serverState;

                if (blockState.hasBlockEntity()) {
                    blockEntity = serverLevel.getBlockEntity(block);
                }
            } else {
                if (!serverState.is(Blocks.SPAWNER)) {
                    blockState = serverState;
                } else {
                    blockState = SpawnerChunkPacketBlockController.decoyState(environment, block.getY());
                }
            }

            packetsToSend.add(new ClientboundBlockUpdatePacket(block, blockState));

            if (blockEntity != null) {
                Packet<ClientGamePacketListener> bePacket = blockEntity.getUpdatePacket();

                if (bePacket != null) {
                    packetsToSend.add(bePacket);
                }
            }
        }

        sendPackets(player, packetsToSend);
    }

    public static void syncServerBlock(SSASpawnerAntiESP plugin, Player player, Location location) {
        PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null || location.getWorld() == null) {
            return;
        }

        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        BlockPos block = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockState blockState = serverLevel.getBlockState(block);
        List<Packet<?>> packets = new ArrayList<>();
        packets.add(new ClientboundBlockUpdatePacket(block, blockState));

        if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(block);

            if (blockEntity != null) {
                Packet<ClientGamePacketListener> bePacket = blockEntity.getUpdatePacket();

                if (bePacket != null) {
                    packets.add(bePacket);
                }
            }
        }

        sendPackets(player, packets);
    }

    public static void removeBlockFromPlayerState(PlayerData playerData, BlockPos block) {
        for (ChunkBlocks chunkBlocks : playerData.getChunks().values()) {
            chunkBlocks.getBlocks().remove(block);
        }

        Queue<Result> results = playerData.getResults();
        Iterator<Result> iterator = results.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().getBlock().equals(block)) {
                iterator.remove();
            }
        }
    }

    private static void sendPackets(Player player, List<Packet<?>> packets) {
        if (packets.isEmpty()) {
            return;
        }

        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        if (connection == null || NmsCompat.isConnectionDisconnected(connection)) {
            return;
        }

        for (Packet<?> packet : packets) {
            connection.send(packet);
        }
    }
}
