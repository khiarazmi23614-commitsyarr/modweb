package com.khiar.modweb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModWebConfig {
    public String keybind = "F9";
    public int width = 1100;
    public int height = 700;
    public int x = 40;
    public int y = 40;
    public boolean muteAudio = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("modweb.json");

    public static ModWebConfig load() {
        try {
            if (Files.exists(PATH)) return GSON.fromJson(Files.readString(PATH), ModWebConfig.class);
        } catch (Exception ignored) { }
        ModWebConfig config = new ModWebConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) { }
    }
}
