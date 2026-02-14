package com.techphonesnews.HyUniverseThread;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class HyUniverseThread extends JavaPlugin {

    public HyUniverseThread(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().atInfo().log("MyPlugin was successfully setup!");
        super.setup();
    }

    @Override
    protected void start() {
        // You can disable core plugins here
        // Uncomment to disable Hytale's Teleport plugin
        //PluginIdentifier identifier = PluginIdentifier.fromString("Hytale:Teleport");
        //HytaleServer.get().getPluginManager().unload(identifier);
        // You can also override previously registered command here,
        // by registering in "start()" instead of "setup()"
        super.start();
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("MyPlugin was successfully shutdown!");
        super.shutdown();
    }
}