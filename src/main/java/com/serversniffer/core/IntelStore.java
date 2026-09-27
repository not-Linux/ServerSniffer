package com.serversniffer.core;

/** Global singleton — survives module toggling, reset on disconnect. */
public final class IntelStore {
    private static final ServerIntel INTEL = new ServerIntel();

    private IntelStore() {}

    public static ServerIntel get() { return INTEL; }
    public static void reset() { INTEL.reset(); }
}
