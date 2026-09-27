package com.serversniffer.util;

import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.text.Text;

/**
 * Actively pings the current server's status endpoint to pull
 * MOTD, version, player counts and favicon without needing a separate connection.
 * Called automatically on join and on demand via .scan.
 */
public final class ServerPinger {

    private ServerPinger() {}

    public static void pingCurrent() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry == null) return;
        ping(entry.address);
    }

    public static void ping(String address) {
        ServerIntel intel = IntelStore.get();
        try {
            // Use Minecraft's own pinger — it handles SRV, ping, handshake, status correctly
            ServerInfo info = new ServerInfo("sniff", address, ServerInfo.ServerType.OTHER);
            MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();

            // Callback populates info fields
            pinger.add(info, () -> {
                // Runs on network thread — copy off to main
                MinecraftClient.getInstance().execute(() -> {
                    if (info.label != null) {
                        intel.motdRaw = info.label.getString();
                        intel.motdStripped = TextUtil.toStrippedString(info.label);
                    }
                    if (info.version != null) {
                        intel.minecraftVersion = info.version.getString();
                        // info.protocolVersion via reflection — field name is protocolVersion
                        try {
                            var f = info.getClass().getField("protocolVersion");
                            intel.protocolVersion = f.getInt(info);
                        } catch (Exception e) {
                            try {
                                var f2 = info.getClass().getDeclaredField("protocolVersion");
                                f2.setAccessible(true);
                                intel.protocolVersion = f2.getInt(info);
                            } catch (Exception ignored) {}
                        }
                    }
                    intel.onlinePlayers = (int) info.players.online();
                    intel.maxPlayers = (int) info.players.max();
                    // icon base64 if present
                    try {
                        var iconField = info.getClass().getDeclaredField("icon");
                        iconField.setAccessible(true);
                        Object icon = iconField.get(info);
                        if (icon != null) intel.serverIconBase64 = icon.toString();
                    } catch (Exception ignored) {}

                    intel.log("Status ping: " + intel.minecraftVersion + " " + intel.onlinePlayers + "/" + intel.maxPlayers);

                    // Heuristic: version string often contains proxy/software hints like "Velocity 1.7-1.21"
                    String v = intel.minecraftVersion;
                    if (v != null) {
                        String low = v.toLowerCase();
                        if (low.contains("velocity")) { intel.proxy = "Velocity"; intel.proxyEvidence.add("version string: " + v); }
                        if (low.contains("bungee") || low.contains("waterfall")) { intel.proxy = "BungeeCord/Waterfall"; intel.proxyEvidence.add("version string: " + v); }
                        // ViaVersion puts multiple versions in the string e.g. "1.8-1.21.4"
                        if (low.matches(".*1\\.\\d+.*-.*1\\.\\d+.*") && low.contains("-")) {
                            intel.viaVersionDetected = true;
                            intel.viaEvidence = "status version range: " + v;
                        }
                    }
                });
            }, () -> {
                // failure callback — still log it
                intel.log("Status ping failed for " + address);
            });
        } catch (Exception e) {
            intel.log("Pinger error: " + e.getMessage());
        }
    }
}
