package com.serversniffer.mixin;

import com.serversniffer.core.IntelStore;
import com.serversniffer.core.ServerIntel;
import com.serversniffer.fingerprint.AnticheatDetector;
import com.serversniffer.fingerprint.PluginDetector;
import com.serversniffer.fingerprint.SoftwareFingerprinter;
import com.serversniffer.util.TextUtil;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowest-level tap — sees every S2C packet before it is handled.
 * Lightweight: no heavy work here, just classify + delegate.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"))
    private void onPacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        // Only care about server -> client packets while playing
        // We do not filter by listener type — cheap instanceof is enough
        ServerIntel intel = IntelStore.get();
        String name = packet.getClass().getSimpleName();
        intel.increment(name);
        intel.log("S2C " + name);

        // ── KeepAlive cadence ──
        if (packet instanceof KeepAliveS2CPacket) {
            long now = System.currentTimeMillis();
            // keep last 20 intervals
            if (intel.keepAliveIntervals.isEmpty()) {
                intel.keepAliveIntervals.add(now);
            } else {
                long last = intel.keepAliveIntervals.get(intel.keepAliveIntervals.size() - 1);
                long delta = now - last;
                // store delta, but keep a timestamp list — we convert to deltas lazily
                // To get proper intervals we store deltas instead of absolutes
                // Easier: just record delta if it looks sane (< 60s)
                if (delta > 200 && delta < 60_000) {
                    // we keep a separate list of deltas — reuse keepAliveIntervals for deltas after first
                    // first entry is absolute, rest are deltas
                    intel.keepAliveIntervals.add(delta);
                    if (intel.keepAliveIntervals.size() > 21) intel.keepAliveIntervals.remove(1);
                    // compute avg over deltas
                    double sum = 0; int n = 0;
                    for (int i = 1; i < intel.keepAliveIntervals.size(); i++) { sum += intel.keepAliveIntervals.get(i); n++; }
                    if (n > 0) intel.avgKeepAliveMs = sum / n;
                } else {
                    // still push timestamp for next delta if we lost sync
                    if (intel.keepAliveIntervals.size() == 1) {
                        // keep single timestamp, wait for next
                    }
                }
            }
            intel.log("  keepalive id=" + ((KeepAliveS2CPacket) packet).getId());
            return;
        }

        if (packet instanceof CommonPingS2CPacket ping) {
            intel.log("  ping id=" + ping.getParameter());
            return;
        }

        // ── Custom payloads — richest signal for software / plugins / AC ──
        if (packet instanceof CustomPayloadS2CPacket cpc) {
            var payload = cpc.payload();
            String id = payload.getId().id().toString(); // Identifier
            intel.channels.add(id);
            intel.log("  custom payload channel=" + id);

            PluginDetector.onChannel(intel, id);
            AnticheatDetector.onChannel(intel, id);
            SoftwareFingerprinter.onChannelHint(intel, id);

            // brand channel carries the server brand string
            if (id.equals("minecraft:brand")) {
                try {
                    // Brand payload: net.minecraft.network.packet.BrandCustomPayload
                    // Access via reflection-friendly to avoid hard dep on exact class name
                    // In Yarn 1.21.4 the payload class is net.minecraft.network.packet.BrandCustomPayload
                    Object brandPayload = payload;
                    // try to get brand string via toString or known getter
                    String brand = null;
                    try {
                        var m = brandPayload.getClass().getMethod("brand");
                        Object v = m.invoke(brandPayload);
                        brand = String.valueOf(v);
                    } catch (Exception ignored) {
                        // fallback: parse packet bytes via TextUtil from payload's string repr
                        brand = brandPayload.toString();
                    }
                    if (brand != null && !brand.isBlank()) {
                        SoftwareFingerprinter.onBrand(intel, brand);
                        intel.log("  brand=\"" + brand + "\"");
                    }
                } catch (Exception e) {
                    intel.log("  brand parse failed: " + e.getMessage());
                }
            }

            // minecraft:register — payload is a \0-separated list of channels the server listens on
            if (id.equals("minecraft:register")) {
                try {
                    String channelsStr = extractRegisterChannels(payload);
                    if (channelsStr != null && !channelsStr.isBlank()) {
                        for (String ch : channelsStr.split("\0")) {
                            if (ch.isBlank()) continue;
                            intel.channels.add(ch);
                            PluginDetector.onChannel(intel, ch);
                            AnticheatDetector.onChannel(intel, ch);
                            SoftwareFingerprinter.onChannelHint(intel, ch);
                            intel.log("    register channel: " + TextUtil.prettyId(ch));
                        }
                    }
                } catch (Exception e) {
                    intel.log("  register parse failed: " + e.getMessage());
                }
            }
        }
    }

    private static String extractRegisterChannels(Object payload) {
        // payload for minecraft:register is net.minecraft.network.packet.CustomPayload with bytes
        // Try common accessors
        try {
            // Newer versions wrap in RegisterCustomPayload
            var m = payload.getClass().getMethod("channels");
            Object v = m.invoke(payload);
            if (v instanceof String s) return s;
            // or List<Identifier>
            if (v instanceof java.util.Collection<?> c) {
                StringBuilder sb = new StringBuilder();
                for (Object o : c) sb.append(o.toString()).append('\0');
                return sb.toString();
            }
        } catch (Exception ignored) {}
        try {
            // fallback via field "channels" or "data"
            for (var f : payload.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(payload);
                if (v instanceof String s) return s;
                if (v instanceof byte[] b) return new String(b, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return payload.toString();
    }

    @Inject(method = "setPacketListener", at = @At("HEAD"))
    private void onSetListener(PacketListener listener, CallbackInfo ci) {
        // reset inte when a new game listener is set (re-join / server switch)
        // we do NOT reset here blindly — the play handler mixin handles proper reset
    }
}
