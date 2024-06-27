package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Path;
import java.util.UUID;

public class SkinIO {
    
    private static final String FILE_EXTENSION = ".json";
    
    private final Path savePath;
    
    public SkinIO(Path savePath) {
        this.savePath = savePath;
    }
    
    public boolean skinExists(UUID uuid) {
        return savePath.resolve(uuid + FILE_EXTENSION).toFile().exists();
    }
    
    public Property loadSkin(UUID uuid) {
        return JsonUtils.fromJson(FileUtils.readFile(savePath.resolve(uuid + FILE_EXTENSION).toFile()), Property.class);
    }
    
    public void saveSkin(UUID uuid, Property skin) {
        FileUtils.writeFile(savePath.toFile(), uuid + FILE_EXTENSION, JsonUtils.toJson(skin));
    }
}
