package com.techphonesnews.HyUniverseThread;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;

public class HyUniverseThread extends JavaPlugin {

    private static HyUniverseThread INSTANCE;

    private UniverseThread universeThread;
    private final Config<HyUniverseConfig> config;

    public HyUniverseThread(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
        config = this.withConfig(UniverseThread.name, HyUniverseConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        universeThread = new UniverseThread(config.get());
        universeThread.setup();
    }

    @Override
    protected void start() {
        super.start();
        universeThread.start();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        universeThread.stop();
        config.save();
    }

    public static HyUniverseThread get() {
        return INSTANCE;
    }

}