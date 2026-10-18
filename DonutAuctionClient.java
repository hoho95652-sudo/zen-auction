package com.example.donutauction;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DonutAuctionClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauction.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.category.donutauction"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new AuctionScreen());
                }
            }
            AuctionManager.tick(client);
        });

        // Servers deliver chat either as signed chat or as system/game messages; listen to both.
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (!overlay) AuctionManager.onChat(msg.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((msg, signed, sender, params, ts) ->
                AuctionManager.onChat(msg.getString()));
    }
}
