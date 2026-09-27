package com.serversniffer.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** All intel we have gathered about the current server — one instance per connection. */
public class ServerIntel {

    // ── identity ──
    public String ip = "unknown";
    public int port = 25565;
    public String motdRaw = null;
    public String motdStripped = null;
    public int protocolVersion = -1;
    public String minecraftVersion = "unknown";
    public int onlinePlayers = -1;
    public int maxPlayers = -1;
    public String serverIconBase64 = null; // null = no icon / not fetched

    // ── software / brand ──
    /** Raw brand string from minecraft:brand (e.g. "Paper", "Purpur 1.21.4-2318", "Velocity") */
    public String brandRaw = null;
    /** Normalised server software guess */
    public String software = "Unknown";
    /** Confidence 0-100 */
    public int softwareConfidence = 0;
    /** Extra brand evidence */
    public final List<String> brandEvidence = Collections.synchronizedList(new ArrayList<>());

    // ── proxy ──
    public String proxy = "None detected"; // BungeeCord / Velocity / None
    public final List<String> proxyEvidence = Collections.synchronizedList(new ArrayList<>());

    // ── plugin channels ──
    /** Raw channel ids seen on minecraft:register / custom payloads */
    public final Set<String> channels = ConcurrentHashMap.newKeySet();
    /** Inferred plugins (name -> evidence channel) */
    public final Map<String, String> inferredPlugins = new ConcurrentHashMap<>();

    // ── anticheat ──
    public final Set<String> anticheats = ConcurrentHashMap.newKeySet();
    public final List<String> anticheatEvidence = Collections.synchronizedList(new ArrayList<>());

    // ── Via* / protocol translation ──
    public boolean viaVersionDetected = false;
    public String viaEvidence = null;

    // ── distances & limits ──
    public int viewDistance = -1;
    public int simulationDistance = -1;
    public int compressionThreshold = -1;

    // ── world ──
    public String dimension = null;
    public String dimensionType = null;
    public long seedHash = 0; // 0 = unknown (we never learn real seed)
    public boolean reducedDebugInfo = false;
    public boolean doImmediateRespawn = false;
    public String difficulty = null;
    public boolean hardcore = false;

    // ── security ──
    public boolean enforcesSecureChat = false;
    public boolean hasChatPreview = false;

    // ── timings ──
    public long joinedAtMs = 0;
    public final List<Long> keepAliveIntervals = Collections.synchronizedList(new ArrayList<>());
    public double avgKeepAliveMs = -1;
    public int tpsEstimate = -1; // derived from keepalive / tick deltas

    // ── packet stats ──
    public final Map<String, Integer> packetCounts = new ConcurrentHashMap<>();
    public long totalPackets = 0;

    // ── raw log for "everything" view ──
    public final List<String> packetLog = Collections.synchronizedList(new ArrayList<>());
    public static final int MAX_LOG = 400;

    public void log(String line) {
        packetLog.add(line);
        if (packetLog.size() > MAX_LOG) packetLog.remove(0);
        increment("LOG:" + line.substring(0, Math.min(line.length(), 40)));
    }

    public void increment(String packetName) {
        packetCounts.merge(packetName, 1, Integer::sum);
        totalPackets++;
    }

    public void reset() {
        ip = "unknown"; port = 25565; motdRaw = null; motdStripped = null;
        protocolVersion = -1; minecraftVersion = "unknown"; onlinePlayers = -1; maxPlayers = -1;
        brandRaw = null; software = "Unknown"; softwareConfidence = 0; brandEvidence.clear();
        proxy = "None detected"; proxyEvidence.clear();
        channels.clear(); inferredPlugins.clear();
        anticheats.clear(); anticheatEvidence.clear();
        viaVersionDetected = false; viaEvidence = null;
        viewDistance = -1; simulationDistance = -1; compressionThreshold = -1;
        dimension = null; dimensionType = null; seedHash = 0; reducedDebugInfo = false;
        doImmediateRespawn = false; difficulty = null; hardcore = false;
        enforcesSecureChat = false; hasChatPreview = false;
        joinedAtMs = System.currentTimeMillis();
        keepAliveIntervals.clear(); avgKeepAliveMs = -1; tpsEstimate = -1;
        packetCounts.clear(); totalPackets = 0; packetLog.clear();
    }

    public String summaryLine() {
        return String.format("%s @ %s:%d  |  %s  |  %d plugins, %d ACs, %d channels",
            software, ip, port, proxy, inferredPlugins.size(), anticheats.size(), channels.size());
    }
}
