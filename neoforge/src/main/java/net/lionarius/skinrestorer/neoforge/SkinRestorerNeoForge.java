package net.lionarius.skinrestorer.neoforge;

import net.lionarius.skinrestorer.SkinRestorer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(SkinRestorer.MOD_ID)
public final class SkinRestorerNeoForge {
    
    public SkinRestorerNeoForge() {
        SkinRestorer.onInitialize(FMLPaths.CONFIGDIR.get());
    }
}
