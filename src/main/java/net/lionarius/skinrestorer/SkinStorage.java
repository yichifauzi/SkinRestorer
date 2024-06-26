package net.lionarius.skinrestorer;

import com.mojang.authlib.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinStorage {

    private final Map<UUID, Property> skinMap = new HashMap<>();
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

        return skinMap.get(uuid);
    }

    public void removeSkin(UUID uuid) {
        if (skinMap.containsKey(uuid)) {
            skinIO.saveSkin(uuid, skinMap.get(uuid));
        }
    }

    public void setSkin(UUID uuid, Property skin) {
        skinMap.put(uuid, skin);
    }
}
