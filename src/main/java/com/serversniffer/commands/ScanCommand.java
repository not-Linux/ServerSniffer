package com.serversniffer.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.serversniffer.core.IntelStore;
import com.serversniffer.util.ServerPinger;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

/**
 * .scan [address] — actively pings a server's status endpoint and merges results into intel.
 * No arg = scans the server you're currently on.
 * With arg = scans any address (does not connect, just pings status + SRV lookup).
 */
public class ScanCommand extends Command {

    public ScanCommand() {
        super("scan", "Actively pings a server's status to grab MOTD, version & counts (no full join).", "ping", "probe");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            var mc = MinecraftClient.getInstance();
            var entry = mc.getCurrentServerEntry();
            if (entry == null) {
                error("Not on a server. Use .scan <address> to scan a remote server.");
                return SINGLE_SUCCESS;
            }
            info("Scanning " + entry.address + " … (status ping)");
            ServerPinger.ping(entry.address);
            info("Ping sent — run .serverinfo in a second to see updated info.");
            return SINGLE_SUCCESS;
        });

        builder.then(argument("address", StringArgumentType.greedyString())
            .executes(ctx -> {
                String addr = StringArgumentType.getString(ctx, "address").trim();
                if (addr.isEmpty()) { error("Usage: .scan <address[:port]>"); return SINGLE_SUCCESS; }
                info("Scanning " + addr + " … (status ping — you won't join the server)");
                IntelStore.get().log("Manual scan: " + addr);
                ServerPinger.ping(addr);
                info("Ping sent — run .serverinfo shortly to see results.");
                return SINGLE_SUCCESS;
            }));
    }
}
