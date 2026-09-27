package com.serversniffer.fingerprint;

import com.serversniffer.core.ServerIntel;
import java.util.LinkedHashMap;
import java.util.Map;

/** Map of plugin channels -> plugin name. Kept sorted by popularity for a nicer report. */
public final class PluginDetector {

    /** channel (lowercase) -> plugin display name */
    private static final Map<String, String> CHANNEL_TO_PLUGIN = new LinkedHashMap<>();

    static {
        // Mechanics / gameplay
        CHANNEL_TO_PLUGIN.put("worldguard", "WorldGuard");
        CHANNEL_TO_PLUGIN.put("worldedit", "WorldEdit / FAWE");
        CHANNEL_TO_PLUGIN.put("lwc", "LWC");
        CHANNEL_TO_PLUGIN.put("coreprotect", "CoreProtect");
        CHANNEL_TO_PLUGIN.put("griefprevention", "GriefPrevention");
        CHANNEL_TO_PLUGIN.put("preciousstones", "PreciousStones");
        CHANNEL_TO_PLUGIN.put("plotsquared", "PlotSquared");
        CHANNEL_TO_PLUGIN.put("essentials", "EssentialsX");
        CHANNEL_TO_PLUGIN.put("essentials:msg", "EssentialsX");

        // Economy / shops
        CHANNEL_TO_PLUGIN.put("vault", "Vault");
        CHANNEL_TO_PLUGIN.put("shopguiplus", "ShopGUIPlus");
        CHANNEL_TO_PLUGIN.put("chestshop", "ChestShop");
        CHANNEL_TO_PLUGIN.put("quickshop", "QuickShop");

        // Chat / nicks
        CHANNEL_TO_PLUGIN.put("chatmanager", "ChatManager");
        CHANNEL_TO_PLUGIN.put("venturechat", "VentureChat");
        CHANNEL_TO_PLUGIN.put("chatcontrol", "ChatControl");

        // Anticheat channels (also picked up by AnticheatDetector, but listed here for completeness)
        CHANNEL_TO_PLUGIN.put("aac", "AAC (Advanced AntiCheat)");
        CHANNEL_TO_PLUGIN.put("nocheatplus", "NoCheatPlus");
        CHANNEL_TO_PLUGIN.put("spartan", "Spartan AntiCheat");
        CHANNEL_TO_PLUGIN.put("matrix", "Matrix AntiCheat");
        CHANNEL_TO_PLUGIN.put("vulcan", "Vulcan AntiCheat");
        CHANNEL_TO_PLUGIN.put("grim", "GrimAC");
        CHANNEL_TO_PLUGIN.put("intave", "Intave");

        // Client / mod support
        CHANNEL_TO_PLUGIN.put("wecui", "WorldEditCUI");
        CHANNEL_TO_PLUGIN.put("worldedit:cui", "WorldEditCUI");
        CHANNEL_TO_PLUGIN.put("lunarclient", "Lunar Client integration");
        CHANNEL_TO_PLUGIN.put("badlion", "Badlion Client integration");

        // Proxy / cross-server
        CHANNEL_TO_PLUGIN.put("bungeecord:main", "BungeeCord messaging");
        CHANNEL_TO_PLUGIN.put("velocity:player_info", "Velocity player-info");
        CHANNEL_TO_PLUGIN.put("redisbungee", "RedisBungee");

        // Common library channels
        CHANNEL_TO_PLUGIN.put("fml", "Forge Mod Loader");
        CHANNEL_TO_PLUGIN.put("fml:handshake", "Forge handshake");
    }

    private PluginDetector() {}

    /** Call whenever a new channel is observed. */
    public static void onChannel(ServerIntel intel, String channel) {
        String low = channel.toLowerCase();

        // Exact match first
        String plugin = CHANNEL_TO_PLUGIN.get(low);
        if (plugin != null) {
            intel.inferredPlugins.putIfAbsent(plugin, channel);
            return;
        }

        // Prefix / contains heuristics for namespaced channels like "myplugin:foo"
        String namespace = low.contains(":") ? low.substring(0, low.indexOf(':')) : low;
        plugin = CHANNEL_TO_PLUGIN.get(namespace);
        if (plugin != null) {
            intel.inferredPlugins.putIfAbsent(plugin, channel);
            return;
        }

        // Unknown namespace — still surface it so the user can identify custom plugins
        // We do NOT auto-infer a plugin name for unknowns; just keep the raw channel visible
        // via intel.channels. The report will list unknown channels separately.
    }

    /** Accessor for the map (used by the report to explain known channels). */
    public static Map<String, String> knownMappings() { return CHANNEL_TO_PLUGIN; }
}
