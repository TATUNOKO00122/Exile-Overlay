package com.example;

import com.example.exile_overlay.client.render.orb.OrbRegistry;

public final class ExampleMod {
    public static final String MOD_ID = "exile_overlay";

    public static void init() {
        // Write common init code here.
        OrbRegistry.initialize();
    }
}
