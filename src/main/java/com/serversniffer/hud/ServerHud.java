package com.serversniffer.hud;

import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class ServerHud extends HudElement {

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

    public ServerHud(HudElementInfo<?> info) {
        super(info);
    }

    @Override
    public void tick(HudRenderer renderer) {
        ServerIntel i = IntelStore.get();
        boolean empty = i.ip.equals("unknown") && i.software.equals("Unknown");

        // Estimate box size (must be set in tick, not render)
        // 7 lines compact, 10 full — approximate; renderer.textWidth gives exact per line
        double maxW = renderer.textWidth("ServerSniffer");
        if (empty) {
            maxW = Math.max(maxW, renderer.textWidth("No server data — join a server"));
        } else {
            if (showIp.get()) maxW = Math.max(maxW, renderer.textWidth(i.ip + ":" + i.port));
            maxW = Math.max(maxW, renderer.textWidth(i.software + " (" + i.softwareConfidence + "%)"));
            if (!i.proxy.equals("None detected")) maxW = Math.max(maxW, renderer.textWidth("Proxy: " + i.proxy));
            maxW = Math.max(maxW, renderer.textWidth("Plugins: " + i.inferredPlugins.size() + "  Channels: " + i.channels.size()));
            if (!i.anticheats.isEmpty()) maxW = Math.max(maxW, renderer.textWidth("AC: " + String.join(", ", i.anticheats)));
            if (mode.get() == Mode.Full) {
                if (i.viewDistance != -1) maxW = Math.max(maxW, renderer.textWidth("View " + i.viewDistance + "  Sim " + i.simulationDistance));
                maxW = Math.max(maxW, renderer.textWidth("Packets: " + i.totalPackets));
            }
            maxW = Math.max(maxW, renderer.textWidth(" .serverinfo for full report"));
        }
        box.setSize(maxW + 8, (mode.get() == Mode.Full ? 110 : 80));
    }

    @Override
    public void render(HudRenderer renderer) {
        ServerIntel i = IntelStore.get();
        boolean empty = i.ip.equals("unknown") && i.software.equals("Unknown");

        Color aqua = new Color(85, 255, 255);
        Color gray = new Color(170, 170, 170);
        Color white = new Color(255, 255, 255);
        Color dark = new Color(90, 90, 90);
        Color red = new Color(255, 85, 85);

        double x = box.getX();
        double y = box.getY();

        renderer.text("ServerSniffer", x, y, aqua);
        y += 12;

        if (empty) {
            renderer.text("No server data — join a server", x, y, gray);
            return;
        }

        if (showIp.get()) {
            renderer.text(i.ip + ":" + i.port, x, y, gray);
            y += 10;
        }

        renderer.text(i.software + " (" + i.softwareConfidence + "%)", x, y, white);
        y += 10;

        if (!i.proxy.equals("None detected")) {
            renderer.text("Proxy: " + i.proxy, x, y, gray);
            y += 10;
        }

        renderer.text("Plugins: " + i.inferredPlugins.size() + "  Channels: " + i.channels.size(), x, y, gray);
        y += 10;

        if (!i.anticheats.isEmpty()) {
            renderer.text("AC: " + String.join(", ", i.anticheats), x, y, red);
            y += 10;
        }

        if (mode.get() == Mode.Full) {
            if (i.viewDistance != -1 || i.simulationDistance != -1) {
                renderer.text("View " + (i.viewDistance == -1 ? "?" : i.viewDistance)
                    + "  Sim " + (i.simulationDistance == -1 ? "?" : i.simulationDistance), x, y, dark);
                y += 10;
            }
            renderer.text("Packets: " + i.totalPackets, x, y, dark);
            y += 10;

            if (i.avgKeepAliveMs > 0) {
                renderer.text(String.format("KeepAlive ~%.0f ms", i.avgKeepAliveMs), x, y, dark);
                y += 10;
            }
        }

        renderer.text(" .serverinfo for full report", x, y, dark);
    }
}
