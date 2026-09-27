package com.serversniffer.fingerprint;

import com.serversniffer.core.ServerIntel;
import java.util.LinkedHashMap;
import java.util.Map;

/** Detects anticheats from channels, plugin messages, and behavioural quirks. */
public final class AnticheatDetector {

    private static final Map<String, String> CHANNEL_TO_AC = new LinkedHashMap<>();

    static {
        CHANNEL_TO_AC.put("aac", "AAC");
        CHANNEL_TO_AC.put("nocheatplus", "NoCheatPlus");
        CHANNEL_TO_AC.put("ncp", "NoCheatPlus");
        CHANNEL_TO_AC.put("spartan", "Spartan");
        CHANNEL_TO_AC.put("matrix", "Matrix");
        CHANNEL_TO_AC.put("vulcan", "Vulcan");
        CHANNEL_TO_AC.put("grim", "GrimAC");
        CHANNEL_TO_AC.put("grimac", "GrimAC");
        CHANNEL_TO_AC.put("intave", "Intave");
        CHANNEL_TO_AC.put("negativity", "Negativity");
        CHANNEL_TO_AC.put("anticheat", "Generic AntiCheat");
        CHANNEL_TO_AC.put("verus", "Verus");
        CHANNEL_TO_AC.put("karhu", "Karhu");
        CHANNEL_TO_AC.put("horizon", "Horizon");
        CHANNEL_TO_AC.put("themis", "Themis");
        CHANNEL_TO_AC.put("watchcat", "WatchCat");
        CHANNEL_TO_AC.put("godseye", "GodsEye");
        CHANNEL_TO_AC.put("reflex", "Reflex");
        CHANNEL_TO_AC.put("wraith", "Wraith");
        CHANNEL_TO_AC.put("nope", "NopeAC");
        CHANNEL_TO_AC.put("polar", "Polar");
    }

    private AnticheatDetector() {}

    public static void onChannel(ServerIntel i, String channel) {
        String low = channel.toLowerCase();
        String base = low.contains(":") ? low.substring(0, low.indexOf(':')) : low;
        // check both full and base
        for (Map.Entry<String, String> e : CHANNEL_TO_AC.entrySet()) {
            if (low.contains(e.getKey()) || base.equals(e.getKey())) {
                if (i.anticheats.add(e.getValue())) {
                    i.anticheatEvidence.add(e.getValue() + " via channel " + channel);
                }
            }
        }
    }

    /** Behavioural hints that suggest an AC even without a channel. */
    public static void onBehaviour(ServerIntel i, String hint) {
        if (i.anticheatEvidence.contains(hint)) return;
        i.anticheatEvidence.add(hint);
    }
}
