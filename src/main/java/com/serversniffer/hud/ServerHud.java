package com.serversniffer.hud;

import com.serversniffer.ServerSnifferAddon;
import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class ServerHud extends HudElement {

    public static final HudElementInfo<ServerHud> INFO = new HudElementInfo<>(
        ServerSnifferAddon.HUD_GROUP, "server-hud", "Shows sniffed server intel on-screen.", ServerHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How much to show.")
        .defaultValue(Mode.Compact)
        .build());

    private final Setting<Boolean> showIp = sgGeneral.add(new BoolSetting.Builder()
        .name("show-ip")
        .description("Show server IP on HUD.")
        .defaultValue(true)
        .build());

    public enum Mode { Compact, Full }

    public ServerHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        ServerIntel i = IntelStore.get();
        boolean empty = i.ip.equals("unknown") && i.software.equals("Unknown");

        // Title — measure first to size box
        String title = "ServerSniffer";
        double w = renderer.textWidth(title, true);

        if (empty) {
            String msg = "No server data — join a server";
            w = Math.max(w, renderer.textWidth(msg, true));
            setSize(w, 22);
            renderer.text(title, x, y, Color.fromRGB(85, 255, 255), true);
            renderer.text(msg, x, y + 11, Color.fromRGB(170, 170, 170), true);
            return;
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(title);
        if (showIp.get()) lines.add(i.ip + ":" + i.port);
        lines.add(i.software + " (" + i.softwareConfidence + "%)");
        if (!i.proxy.equals("None detected")) lines.add("Proxy: " + i.proxy);
        lines.add("Plugins: " + i.inferredPlugins.size() + "  Channels: " + i.channels.size());
        if (!i.anticheats.isEmpty()) lines.add("AC: " + String.join(", ", i.anticheats));
        if (mode.get() == Mode.Full) {
            if (i.viewDistance != -1 || i.simulationDistance != -1) {
                lines.add("View " + (i.viewDistance == -1 ? "?" : i.viewDistance) + "  Sim " + (i.simulationDistance == -1 ? "?" : i.simulationDistance));
            }
            lines.add("Packets: " + i.totalPackets);
            if (i.avgKeepAliveMs > 0) lines.add(String.format("KeepAlive ~%.0f ms", i.avgKeepAliveMs));
        }
        lines.add(" .serverinfo for full report");

        for (String s : lines) w = Math.max(w, renderer.textWidth(s, true));
        setSize(w, lines.size() * 11 + 4);

        double yy = y;
        for (int idx = 0; idx < lines.size(); idx++) {
            String s = lines.get(idx);
            Color c;
            if (idx == 0) c = Color.fromRGB(85, 255, 255);
            else if (s.startsWith("AC:")) c = Color.fromRGB(255, 85, 85);
            else if (idx == lines.size() - 1) c = Color.fromRGB(90, 90, 90);
            else c = Color.fromRGB(170, 170, 170);
            renderer.text(s, x, yy, c, true);
            yy += 11;
        }
    }
}
