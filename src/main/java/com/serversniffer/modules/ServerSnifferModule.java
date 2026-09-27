package com.serversniffer.modules;

import com.serversniffer.ServerSnifferAddon;
import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import com.serversniffer.util.ServerPinger;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;

/**
 * The main Meteor module — lives in the ServerSniffer category.
 * Active = sniffing.  Inactive = still collecting but not notifying.
 * Most users will just leave it on.
 */
public class ServerSnifferModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNotify = settings.createGroup("Notifications");
    private final SettingGroup sgScan   = settings.createGroup("Auto-Scan");

    // ── general ──
    public final Setting<Boolean> logPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("log-packets")
        .description("Keep a rolling log of the last 400 S2C packet names. Needed for .serverinfo top-packets.")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> verboseOnJoin = sgGeneral.add(new BoolSetting.Builder()
        .name("verbose-on-join")
        .description("Print a 1-line summary to chat every time you join a server.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> resetOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-on-leave")
        .description("Clear sniffed intel when you leave/disconnect so the next server starts clean.")
        .defaultValue(true)
        .build());

    // ── notifications ──
    private final Setting<Boolean> notifySoftware = sgNotify.add(new BoolSetting.Builder()
        .name("notify-software")
        .description("Toast when server software is fingerprinted.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> notifyAnticheat = sgNotify.add(new BoolSetting.Builder()
        .name("notify-anticheat")
        .description("Toast when an anticheat is detected.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> notifyPlugins = sgNotify.add(new BoolSetting.Builder()
        .name("notify-plugins")
        .description("Chat message when new plugin channels are discovered.")
        .defaultValue(false)
        .build());

    // ── auto-scan ──
    private final Setting<Boolean> autoPing = sgScan.add(new BoolSetting.Builder()
        .name("auto-ping")
        .description("Automatically ping the server's status endpoint 1s after joining to grab MOTD, version & player counts.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> autoPingDelay = sgScan.add(new IntSetting.Builder()
        .name("auto-ping-delay")
        .description("Ticks to wait after join before auto-pinging (20 ticks = 1s).")
        .defaultValue(20)
        .min(0).max(200).sliderMax(100)
        .visible(autoPing::get)
        .build());

    // ── internal ──
    private int ticksSinceJoin = -1;
    private int lastPluginCount = 0;
    private String lastSoftware = "Unknown";
    private int lastACCount = 0;

    public ServerSnifferModule() {
        super(ServerSnifferAddon.CATEGORY, "server-sniffer",
              "Reads server packets to tell you everything about the server — software, proxy, plugins, anticheat, limits, timings & more.");
    }

    @Override
    public void onActivate() {
        ticksSinceJoin = 0;
        lastPluginCount = IntelStore.get().inferredPlugins.size();
        lastSoftware = IntelStore.get().software;
        lastACCount = IntelStore.get().anticheats.size();
        info("Sniffing started — join a server or run §7.serverinfo§f / §7.scan§f.");
    }

    @Override
    public void onDeactivate() {
        info("Sniffing paused (data kept — toggle back on to resume).");
    }

    // ---- events ----

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        ServerIntel intel = IntelStore.get();

        // auto-ping
        if (autoPing.get() && ticksSinceJoin >= 0) {
            ticksSinceJoin++;
            if (ticksSinceJoin == autoPingDelay.get()) {
                ServerPinger.pingCurrent();
            }
            if (ticksSinceJoin > autoPingDelay.get() + 60) ticksSinceJoin = -1; // only once per join
        }

        // software toast (once per upgrade)
        if (notifySoftware.get() && !intel.software.equals(lastSoftware) && !intel.software.equals("Unknown")) {
            info("Software fingerprinted: §a" + intel.software + " §7(" + intel.softwareConfidence + "%)");
            lastSoftware = intel.software;
        }

        // anticheat toast
        if (notifyAnticheat.get() && intel.anticheats.size() > lastACCount) {
            info("Anticheat detected: §c" + String.join("§7, §c", intel.anticheats));
            lastACCount = intel.anticheats.size();
        }

        // plugin notification (rate-limited to changes)
        if (notifyPlugins.get() && intel.inferredPlugins.size() > lastPluginCount) {
            int gained = intel.inferredPlugins.size() - lastPluginCount;
            info("+" + gained + " new plugin(s): §a" + String.join("§7, §a", intel.inferredPlugins.keySet()));
            lastPluginCount = intel.inferredPlugins.size();
        }

        // first-join verbose summary (once, ~3s after join)
        if (verboseOnJoin.get() && ticksSinceJoin == 60 && !intel.software.equals("Unknown")) {
            info("§8[§bServerSniffer§8] §7" + intel.summaryLine() + " §8— run §f.serverinfo §8for details.");
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ticksSinceJoin = -1;
        if (resetOnLeave.get()) {
            // keep the last report briefly so .serverinfo still works on the title screen,
            // but mark it stale — full reset happens on next GameJoin mixin
            IntelStore.get().log("Game left — data will reset on next join.");
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        // Title / disconnect screen = we left the server — schedule reset if enabled
        if (event.screen instanceof TitleScreen || event.screen instanceof DisconnectedScreen) {
            ticksSinceJoin = -1;
        }
        // Connecting to a new server — reset immediately so stale intel doesn't leak
        if (event.screen instanceof ConnectScreen) {
            if (resetOnLeave.get()) IntelStore.reset();
            ticksSinceJoin = 0;
            lastSoftware = "Unknown";
            lastPluginCount = 0;
            lastACCount = 0;
        }
    }

    // called from ClientPlayNetworkHandlerMixin on GameJoin for a clean per-server reset
    public void onServerJoin() {
        ticksSinceJoin = 0;
        lastSoftware = IntelStore.get().software;
        lastPluginCount = IntelStore.get().inferredPlugins.size();
        lastACCount = IntelStore.get().anticheats.size();
    }
}
