package net.lionarius.skinrestorer.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinStorage {
    
    private final Map<UUID, SkinValue> skinMap = new ConcurrentHashMap<>();
    private final SkinIO skinIO;
    
    public SkinStorage(SkinIO skinIO) {
        this.skinIO = skinIO;
    }
    
    public boolean hasSavedSkin(UUID uuid) {
        return this.skinIO.skinExists(uuid);
    }
    
    public SkinValue getSkin(UUID uuid) {
        if (!skinMap.containsKey(uuid)) {
            var skin = skinIO.loadSkin(uuid);
            setSkin(uuid, skin);
        }
        
        return skinMap.get(uuid);
    }
    
    public void removeSkin(UUID uuid) {
        var skin = skinMap.remove(uuid);
        if (skin != null)
            skinIO.saveSkin(uuid, skin);
    }
    
    public void setSkin(UUID uuid, SkinValue skin) {
        if (skin == null)
            skin = SkinValue.EMPTY;
        
        skinMap.put(uuid, skin);
    }
}
