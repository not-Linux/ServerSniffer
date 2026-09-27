package com.serversniffer.fingerprint;

import com.serversniffer.core.ServerIntel;

/** Heuristics to identify the server software from brand + packets. */
public final class SoftwareFingerprinter {

    private SoftwareFingerprinter() {}

    public static void onBrand(ServerIntel i, String brand) {
        i.brandRaw = brand;
        i.brandEvidence.add("brand=\"" + brand + "\"");
        String low = brand.toLowerCase();

        // Direct brand hits — high confidence
        if (low.contains("paper"))        set(i, "Paper", 95);
        else if (low.contains("purpur"))  set(i, "Purpur (Paper fork)", 97);
        else if (low.contains("pufferfish")) set(i, "Pufferfish (Paper fork)", 97);
        else if (low.contains("yatopia")) set(i, "Yatopia", 95);
        else if (low.contains("airplane")) set(i, "Airplane (Paper fork)", 90);
        else if (low.contains("spigot"))  set(i, "Spigot", 90);
        else if (low.contains("bukkit"))  set(i, "CraftBukkit", 90);
        else if (low.contains("fabric"))  set(i, "Fabric", 95);
        else if (low.contains("forge"))   set(i, "Forge / NeoForge", 90);
        else if (low.contains("quilt"))   set(i, "Quilt", 90);
        else if (low.contains("velocity")) set(i, "Velocity (proxy) + backend " + brand, 90);
        else if (low.contains("bungeecord") || low.contains("bungee")) set(i, "BungeeCord (proxy)", 90);
        else if (low.contains("waterfall")) set(i, "Waterfall (Bungee fork)", 90);
        else if (low.contains("sponge"))  set(i, "Sponge", 92);
        else if (low.contains("folia"))   set(i, "Folia (Paper, regionised)", 95);
        else if (low.contains("leaves"))  set(i, "Leaves (Paper fork)", 90);
        else if (low.contains("arcane") || low.contains("arc")) set(i, "Arcane / Custom Paper fork", 75);
        else {
            // Unknown brand string — still record it as a hint
            set(i, "Vanilla / Unknown (brand=" + brand + ")", 40);
        }
    }

    public static void onChannelHint(ServerIntel i, String channel) {
        String c = channel.toLowerCase();
        // Channels that betray software
        if (c.equals("paper:ping_support") || c.startsWith("paper:")) {
            bump(i, "Paper-family", 65, "channel " + channel);
        }
        if (c.equals("velocity:player_info")) {
            // Velocity injects this on backend connections
            i.proxyEvidence.add("channel velocity:player_info");
            i.proxy = "Velocity";
        }
        if (c.equals("bungeecord:main")) {
            i.proxyEvidence.add("channel bungeecord:main");
            i.proxy = "BungeeCord / Waterfall";
        }
    }

    public static void onServerTickHint(ServerIntel i) {
        // If we see Paper's ping-support CustomPayload, it is definitely Paper-family
        if (i.channels.contains("paper:ping_support")) set(i, "Paper-family (confirmed via paper:ping_support)", 98);
    }

    private static void set(ServerIntel i, String name, int conf) {
        // Only upgrade confidence, never downgrade
        if (conf > i.softwareConfidence) {
            i.software = name;
            i.softwareConfidence = conf;
        }
    }

    private static void bump(ServerIntel i, String name, int conf, String evidence) {
        i.brandEvidence.add(evidence);
        set(i, name, conf);
    }
}
