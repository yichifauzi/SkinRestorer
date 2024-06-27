package net.lionarius.skinrestorer.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.lionarius.skinrestorer.SkinRestorer;

public final class SkinRestorerFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        SkinRestorer.onInitialize(FabricLoader.getInstance().getConfigDir());
    }
}
