package com.vanillage.ssaspawnerantiesp.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.data.PlayerSpawnerData;
import com.vanillage.ssaspawnerantiesp.data.VisibilityResult;
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

    public static void hideNearbySpawners(SSASpawnerAntiESP plugin, Player player, PlayerSpawnerData playerData) {
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

        Environment environment = world.getEnvironment();
        List<Packet<?>> packets = new ArrayList<>();

        for (BlockPos block : targets) {
            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }

            playerData.getHiddenByClient().put(PlayerSpawnerData.blockKey(block), true);
            packets.add(new ClientboundBlockUpdatePacket(block, decoyState(environment, block.getY())));
        }

        sendPackets(player, packets);
    }

    @Override
    public void accept(ScheduledTask task) {
        PlayerSpawnerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null || !player.isOnline()) {
            return;
        }

        PlayerSpawnerData.syncEyeFromPlayer(player, playerData);

        World world = player.getWorld();

        if (!plugin.isEnabled(world)) {
            return;
        }

        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Environment environment = world.getEnvironment();
        List<Packet<?>> packets = new ArrayList<>();
        VisibilityResult result;

        while ((result = playerData.getResults().poll()) != null) {
            BlockPos block = result.getBlock();

            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }

            BlockState blockState;
            BlockEntity blockEntity = null;

            if (result.isVisible()) {
                blockState = serverLevel.getBlockState(block);

                if (blockState.getBlock() != Blocks.SPAWNER) {
                    continue;
                }

                if (blockState.hasBlockEntity()) {
                    blockEntity = serverLevel.getBlockEntity(block);
                }
            } else {
                blockState = decoyState(environment, block.getY());
            }

            packets.add(new ClientboundBlockUpdatePacket(block, blockState));

            if (blockEntity != null) {
                Packet<ClientGamePacketListener> bePacket = blockEntity.getUpdatePacket();

                if (bePacket != null) {
                    packets.add(bePacket);
                }
            }
        }

        sendPackets(player, packets);
    }

    private static BlockState decoyState(Environment environment, int y) {
        if (environment == Environment.NETHER) {
            return Blocks.NETHERRACK.defaultBlockState();
        }

        if (environment == Environment.THE_END) {
            return Blocks.END_STONE.defaultBlockState();
        }

        if (y < 0) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }

        return Blocks.STONE.defaultBlockState();
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
