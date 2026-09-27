package com.serversniffer.mixin;

import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.DifficultyS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the play handler to extract world / limit / security fields.
 * Every handler here is S2C and therefore trustworthy intel.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        // Reset on fresh join (leave previous server's intel behind)
        // Keep packet counts but start a new timing window
        i.joinedAtMs = System.currentTimeMillis();

        // ── server address ──
        try {
            var client = MinecraftClient.getInstance();
            var entry = client.getCurrentServerEntry();
            if (entry != null) {
                i.ip = entry.address;
                // port is inside address string "host:port" — keep raw for display
                if (entry.address.contains(":")) {
                    try { i.port = Integer.parseInt(entry.address.substring(entry.address.lastIndexOf(':') + 1)); } catch (Exception ignored) {}
                }
            } else if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
                // singleplayer / open to LAN fallback
                i.ip = client.getNetworkHandler().getConnection().getAddress() != null
                    ? client.getNetworkHandler().getConnection().getAddress().toString() : "singleplayer";
            }
        } catch (Exception ignored) {}

        // ── core fields (handle both old and new packet shapes) ──
        try { i.hardcore = packet.hardcore(); } catch (Throwable ignored) {}
        try { i.maxPlayers = packet.maxPlayers(); } catch (Throwable ignored) {}
        try { i.viewDistance = packet.viewDistance(); } catch (Throwable ignored) {}
        try { i.simulationDistance = packet.simulationDistance(); } catch (Throwable ignored) {}
        try { i.reducedDebugInfo = packet.reducedDebugInfo(); } catch (Throwable ignored) {}
        try { i.enforcesSecureChat = packet.enforcesSecureChat(); } catch (Throwable ignored) {}
        try { i.doImmediateRespawn = packet.showDeathScreen() == false; } catch (Throwable ignored) {}
        // doLimitedCrafting etc not stored
        try {
            var cpsi = packet.commonPlayerSpawnInfo();
            if (cpsi != null) {
                // dimension & seed live inside CommonPlayerSpawnInfo
                try { i.seedHash = cpsi.seed(); } catch (Throwable ignored) {}
                try { i.dimension = cpsi.dimension().getValue().toString(); } catch (Throwable ignored) {}
                try { i.dimensionType = cpsi.dimensionType().getKey().map(k -> k.getValue().toString()).orElse(cpsi.dimensionType().toString()); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        i.log("GameJoin hardcore=" + i.hardcore + " view=" + i.viewDistance + " sim=" + i.simulationDistance + " dim=" + i.dimension);
        // Paper-family ping_support confirmation
        if (i.channels.contains("paper:ping_support")) i.softwareConfidence = Math.max(i.softwareConfidence, 98);
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void onRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        try {
            var cpsi = packet.commonPlayerSpawnInfo();
            if (cpsi != null) {
                try { i.dimension = cpsi.dimension().getValue().toString(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        i.log("Respawn dim=" + i.dimension);
    }

    @Inject(method = "onChunkLoadDistance", at = @At("HEAD"))
    private void onChunkDistance(ChunkLoadDistanceS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        // Use reflection to stay compatible across Yarn mapping variations (distance / chunkLoadDistance / field_...)
        for (var f : packet.getClass().getDeclaredFields()) {
            if (f.getType() == int.class) {
                try { f.setAccessible(true); i.viewDistance = f.getInt(packet); break; } catch (Exception ignored) {}
            }
        }
        i.log("ChunkLoadDistance view=" + i.viewDistance);
    }

    @Inject(method = "onSimulationDistance", at = @At("HEAD"))
    private void onSimDistance(SimulationDistanceS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        for (var f : packet.getClass().getDeclaredFields()) {
            if (f.getType() == int.class) {
                try { f.setAccessible(true); i.simulationDistance = f.getInt(packet); break; } catch (Exception ignored) {}
            }
        }
        i.log("SimulationDistance sim=" + i.simulationDistance);
    }

    @Inject(method = "onDifficulty", at = @At("HEAD"))
    private void onDifficulty(DifficultyS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        try { i.difficulty = packet.getDifficulty().getName(); } catch (Throwable ignored) {
            try { i.difficulty = packet.getDifficulty().toString(); } catch (Throwable ignored2) {}
        }
        i.log("Difficulty " + i.difficulty);
    }

    @Inject(method = "onSynchronizeTags", at = @At("HEAD"))
    private void onTags(SynchronizeTagsS2CPacket packet, CallbackInfo ci) {
        ServerIntel i = IntelStore.get();
        i.log("SynchronizeTags (count=" + packet.getGroups().size() + ")");
        // Tags themselves are not plugin signals, but a huge tag set hints at modded server
        if (packet.getGroups().size() > 800) {
            com.serversniffer.fingerprint.AnticheatDetector.onBehaviour(i, "Large tag set (" + packet.getGroups().size() + " groups) — modded / Paper");
        }
    }

    // disconnect logging handled via GameLeftEvent in the module; no mixin needed here
}
