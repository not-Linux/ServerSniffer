package com.serversniffer.util;

import net.minecraft.text.Text;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final Pattern COLOR_CODES = Pattern.compile("§[0-9a-fk-or]");

    private TextUtil() {}

    public static String strip(String s) {
        if (s == null) return null;
        return COLOR_CODES.matcher(s).replaceAll("");
    }

    public static String toStrippedString(Text text) {
        if (text == null) return null;
        return strip(text.getString());
    }

    public static String bytesToHex(byte[] b, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, max); i++) {
            sb.append(String.format("%02x ", b[i]));
        }
        if (b.length > max) sb.append("…(").append(b.length - max).append(" more)");
        return sb.toString().trim();
    }

    public static String prettyId(String channel) {
        if (channel == null) return "null";
        return channel;
    }
}
