package com.serversniffer.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.serversniffer.core.IntelStore;
import com.serversniffer.report.ReportGenerator;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

/**
 * .serverinfo  — prints the full report
 * .serverinfo copy — copies plaintext report to clipboard
 * .serverinfo dump  — saves to serversniffer/report_<ip>_<ts>.txt
 * .serverinfo clear — clears intel (fresh sniff)
 * .serverinfo log   — dumps rolling packet log
 */
public class ServerInfoCommand extends Command {

    public ServerInfoCommand() {
        super("serverinfo", "Shows everything sniffed about the current server.", "si", "sniff");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ReportGenerator.sendToChat(IntelStore.get());
            return SINGLE_SUCCESS;
        });

        builder.then(literal("copy").executes(ctx -> {
            String text = ReportGenerator.toPlainText(IntelStore.get());
            try {
                MinecraftClient.getInstance().keyboard.setClipboard(text);
                info("Report copied to clipboard.");
            } catch (Exception e) {
                error("Clipboard failed: " + e.getMessage());
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("dump").executes(ctx -> {
            String text = ReportGenerator.toPlainText(IntelStore.get());
            try {
                Path dir = Path.of("serversniffer");
                Files.createDirectories(dir);
                String safeIp = IntelStore.get().ip.replaceAll("[^a-zA-Z0-9._-]", "_");
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path file = dir.resolve("report_" + safeIp + "_" + ts + ".txt");
                Files.writeString(file, text);
                info("Report saved to " + file.toAbsolutePath());
            } catch (Exception e) {
                error("Dump failed: " + e.getMessage());
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(ctx -> {
            IntelStore.reset();
            info("Intel cleared — will repopulate on next packets / ping.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("log").executes(ctx -> {
            var log = IntelStore.get().packetLog;
            if (log.isEmpty()) { info("Packet log is empty (enable log-packets or join a server)."); return SINGLE_SUCCESS; }
            info("— Rolling packet log (last " + log.size() + ") —");
            int start = Math.max(0, log.size() - 60);
            for (int i = start; i < log.size(); i++) {
                info(String.format("%3d ", i + 1) + log.get(i));
            }
            if (start > 0) info("(" + start + " older entries hidden, see .serverinfo dump for full log)");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("channels").executes(ctx -> {
            var ch = IntelStore.get().channels;
            if (ch.isEmpty()) info("No channels seen yet.");
            else {
                info("Channels (" + ch.size() + "):");
                ch.stream().sorted().forEach(c -> info("  · " + c));
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("plugins").executes(ctx -> {
            var m = IntelStore.get().inferredPlugins;
            if (m.isEmpty()) info("No plugins inferred yet (no matching channels).");
            else {
                info("Inferred plugins (" + m.size() + "):");
                m.forEach((p, ch) -> info("  ✔ " + p + " via " + ch));
            }
            var ch = IntelStore.get().channels;
            if (!ch.isEmpty()) {
                long unknown = ch.stream().filter(c -> !m.containsValue(c)).count();
                if (unknown > 0) info("+ " + unknown + " unmapped channel(s) — see .serverinfo channels");
            }
            return SINGLE_SUCCESS;
        }));
    }
}
