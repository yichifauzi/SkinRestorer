package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.properties.Property;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinStorage {
    
    private final Map<UUID, Optional<Property>> skinMap = new ConcurrentHashMap<>();
    private final SkinIO skinIO;
    
    public SkinStorage(SkinIO skinIO) {
        this.skinIO = skinIO;
    }
    
    public boolean hasSavedSkin(UUID uuid) {
        return this.skinIO.skinExists(uuid);
    }
    
    public Property getSkin(UUID uuid) {
        if (!skinMap.containsKey(uuid)) {
            Property skin = skinIO.loadSkin(uuid);
            setSkin(uuid, skin);
        }
        
        return skinMap.get(uuid).orElse(null);
    }
    
    public void removeSkin(UUID uuid) {
        if (skinMap.containsKey(uuid))
            skinIO.saveSkin(uuid, skinMap.get(uuid).orElse(null));
    }
    
    public void setSkin(UUID uuid, Property skin) {
        skinMap.put(uuid, Optional.ofNullable(skin));
    }
}
