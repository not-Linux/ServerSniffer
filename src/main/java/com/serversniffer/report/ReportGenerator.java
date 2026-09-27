package com.serversniffer.report;

import com.serversniffer.core.ServerIntel;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.time.Instant;
import java.util.*;

/**
 * Builds the pretty chat / file report.
 * Everything here is presentation — no packet logic.
 */
public final class ReportGenerator {

    private ReportGenerator() {}

    public static List<Text> buildReport(ServerIntel i) {
        List<Text> out = new ArrayList<>();

        out.add(line("━".repeat(42), Formatting.DARK_GRAY));
        out.add(centered("  SERVER SNIFFER  —  full packet report  ", Formatting.AQUA, Formatting.BOLD));
        out.add(line("━".repeat(42), Formatting.DARK_GRAY));
        out.add(info("Address", i.ip + ":" + i.port));
        out.add(info("MOTD", i.motdStripped != null ? i.motdStripped : "(not captured — rejoin to capture via ServerPinger)"));
        out.add(info("Minecraft", i.minecraftVersion + (i.protocolVersion != -1 ? "  (protocol " + i.protocolVersion + ")" : "")));
        out.add(info("Players", (i.onlinePlayers != -1 ? i.onlinePlayers + "/" + i.maxPlayers : "unknown")));
        out.add(blank());

        // Software
        String swColor = i.softwareConfidence >= 90 ? "§a" : i.softwareConfidence >= 60 ? "§e" : "§7";
        out.add(header(" Software / Brand "));
        out.add(kv("Software", swColor + i.software + " §8(" + i.softwareConfidence + "% confidence)"));
        if (i.brandRaw != null) out.add(kv("  brand", "§7\"" + i.brandRaw + "\""));
        if (!i.brandEvidence.isEmpty()) out.add(kv("  evidence", "§7" + String.join("  §8·  §7", i.brandEvidence)));
        out.add(blank());

        // Proxy
        out.add(header(" Proxy "));
        out.add(kv("Proxy", i.proxy));
        if (!i.proxyEvidence.isEmpty()) out.add(kv("  evidence", "§7" + String.join("  §8·  §7", i.proxyEvidence)));
        out.add(blank());

        // Channels & plugins
        out.add(header(" Plugin channels  (" + i.channels.size() + ") "));
        if (i.channels.isEmpty()) {
            out.add(kv("  (none seen yet — wait a few seconds after join)", ""));
        } else {
            // Known plugins
            if (!i.inferredPlugins.isEmpty()) {
                out.add(kv("  Inferred plugins (" + i.inferredPlugins.size() + ")", ""));
                i.inferredPlugins.forEach((plug, ch) ->
                    out.add(kv("    §a✔ §f" + plug, "§8via §7" + ch)));
            }
            // Unknown / unmapped channels
            Set<String> mapped = new HashSet<>(i.inferredPlugins.values());
            List<String> unknowns = new ArrayList<>();
            for (String ch : i.channels) if (!mapped.contains(ch)) unknowns.add(ch);
            if (!unknowns.isEmpty()) {
                unknowns.sort(String::compareToIgnoreCase);
                out.add(kv("  Other channels (" + unknowns.size() + ")", ""));
                for (String ch : unknowns) out.add(kv("    §8· §7" + ch, ""));
            }
        }
        out.add(blank());

        // Anticheat
        out.add(header(" Anticheat "));
        if (i.anticheats.isEmpty() && i.anticheatEvidence.isEmpty()) {
            out.add(kv("  None detected (no AC channels seen)", ""));
        } else {
            if (!i.anticheats.isEmpty()) out.add(kv("  Detected", "§c" + String.join("§7, §c", i.anticheats)));
            for (String ev : i.anticheatEvidence) out.add(kv("    §8· §7" + ev, ""));
        }
        out.add(blank());

        // Via* / protocol translation
        out.add(header(" Protocol translation "));
        if (i.viaVersionDetected) out.add(kv("  ViaVersion/ViaBackwards", "§eDetected §8— " + i.viaEvidence));
        else out.add(kv("  No Via translation detected", ""));
        out.add(blank());

        // Limits
        out.add(header(" Limits & distances "));
        out.add(kv("View distance", val(i.viewDistance)));
        out.add(kv("Simulation distance", val(i.simulationDistance)));
        out.add(kv("Compression threshold", i.compressionThreshold == -1 ? "unknown (default 256)" : String.valueOf(i.compressionThreshold)));
        out.add(blank());

        // World
        out.add(header(" World "));
        out.add(kv("Dimension", i.dimension != null ? i.dimension : "unknown"));
        out.add(kv("Dimension type", i.dimensionType != null ? i.dimensionType : "unknown"));
        out.add(kv("Seed hash", i.seedHash != 0 ? Long.toHexString(i.seedHash) : "hidden (not sent to client)"));
        out.add(kv("Difficulty", i.difficulty != null ? i.difficulty : "unknown"));
        out.add(kv("Hardcore", String.valueOf(i.hardcore)));
        out.add(kv("Reduced debug info", String.valueOf(i.reducedDebugInfo)));
        out.add(kv("DoImmediateRespawn", String.valueOf(i.doImmediateRespawn)));
        out.add(blank());

        // Security
        out.add(header(" Security "));
        out.add(kv("Enforces secure chat", String.valueOf(i.enforcesSecureChat)));
        out.add(kv("Has chat preview", String.valueOf(i.hasChatPreview)));
        if (i.enforcesSecureChat) out.add(kv("  §7→ Chat signing required — unsigned messages may be rejected", ""));
        out.add(blank());

        // Timings
        out.add(header(" Timings "));
        if (i.joinedAtMs != 0) {
            long age = (System.currentTimeMillis() - i.joinedAtMs) / 1000;
            out.add(kv("Connected for", age + "s  (since " + Instant.ofEpochMilli(i.joinedAtMs) + ")"));
        }
        if (i.avgKeepAliveMs > 0) out.add(kv("Avg KeepAlive interval", String.format("%.0f ms", i.avgKeepAliveMs)));
        out.add(kv("Total S2C packets", String.valueOf(i.totalPackets)));
        // top 8 packet types
        if (!i.packetCounts.isEmpty()) {
            out.add(kv("  Top packets", ""));
            i.packetCounts.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(8)
                .forEach(e -> out.add(kv("    §8· §7" + e.getKey(), "§8×" + e.getValue())));
        }
        out.add(blank());

        out.add(line("━".repeat(42), Formatting.DARK_GRAY));
        out.add(Text.literal("  Tip: §7.serverinfo copy  §8— copies this report to clipboard   §7.serverinfo dump §8— saves to file").formatted(Formatting.GRAY));
        out.add(line("━".repeat(42), Formatting.DARK_GRAY));
        return out;
    }

    /** Sends the report to chat (paged). */
    public static void sendToChat(ServerIntel i) {
        for (Text t : buildReport(i)) ChatUtils.sendMsg(t);
    }

    public static String toPlainText(ServerIntel i) {
        StringBuilder sb = new StringBuilder();
        for (Text t : buildReport(i)) sb.append(t.getString()).append("\n");
        return sb.toString();
    }

    // ── helpers ──
    private static String val(int v) { return v == -1 ? "unknown" : String.valueOf(v); }
    private static Text header(String s) { return Text.literal("§8[§b" + s + "§8]").formatted(Formatting.GRAY); }
    private static Text kv(String k, String v) { return Text.literal("  §7" + k + (v.isEmpty() ? "" : " §8— §f" + v)); }
    private static Text info(String k, String v) { return Text.literal(" §7" + k + " §8— §f" + v); }
    private static Text line(String s, Formatting f) { return Text.literal(s).formatted(f); }
    private static Text centered(String s, Formatting... f) { return Text.literal(s).formatted(f); }
    private static Text blank() { return Text.literal(""); }
}
