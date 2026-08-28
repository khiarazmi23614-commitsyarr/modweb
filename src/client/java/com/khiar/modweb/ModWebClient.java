package com.khiar.modweb;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ModWebClient implements ClientModInitializer {
    private static ModWebConfig config;
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        config = ModWebConfig.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modweb.open_browser",
                InputUtil.Type.KEYSYM,
                keyCode(config.keybind),
                "key.categories.misc"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ModWebClient::tick);
    }

    private static void tick(MinecraftClient client) {
        while (openKey.wasPressed()) {
            if (client.currentScreen instanceof BrowserScreen) {
                client.setScreen(null);
            } else {
                client.setScreen(new BrowserScreen(Text.literal("Google Chrome"), config));
            }
        }
    }

    private static int keyCode(String name) {
        if (name == null) return GLFW.GLFW_KEY_F9;
        try {
            return (int) GLFW.class.getField("GLFW_KEY_" + name.toUpperCase()).get(null);
        } catch (Exception ignored) {
            return GLFW.GLFW_KEY_F9;
        }
    }
}
