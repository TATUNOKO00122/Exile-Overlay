package com.example.exile_overlay.fabric.client;

import net.fabricmc.api.ClientModInitializer;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DamagePopupFabricHandler.register();
    }
}
