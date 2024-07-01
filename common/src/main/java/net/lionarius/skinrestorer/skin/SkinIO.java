package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class SkinIO {
    
    public static final String FILE_EXTENSION = ".json";
    
    private final Path savePath;
    
    public SkinIO(Path savePath) {
        this.savePath = savePath;
    }
    
    public boolean skinExists(UUID uuid) {
        return savePath.resolve(uuid + FILE_EXTENSION).toFile().exists();
    }
    
    public SkinValue loadSkin(UUID uuid) {
        try {
            var value = SkinIO.loadSkin(savePath.resolve(uuid + FILE_EXTENSION).toFile());
            Objects.requireNonNull(value.provider());
            return value;
        } catch (Exception e) {
            return SkinValue.EMPTY;
        }
    }
    
    private static SkinValue loadSkin(File file) {
        var json = FileUtils.readFile(file);
        try {
            return JsonUtils.fromJson(json, SkinValue.class);
        } catch (Exception e) {
            var property = JsonUtils.fromJson(json, Property.class);
            return SkinIO.convertFromOldFormat(property);
        }
    }
    
    public void saveSkin(UUID uuid, SkinValue skin) {
        FileUtils.writeFile(savePath.toFile(), uuid + FILE_EXTENSION, JsonUtils.toJson(skin));
    }
    
    private static SkinValue convertFromOldFormat(Property property) {
        try {
            var propertyJson = Objects.requireNonNull(JsonUtils.skinPropertyToJson(property));
            var textures = propertyJson.getAsJsonObject("textures");
            
            var capeTexture = textures.getAsJsonObject("CAPE");
            if (capeTexture != null) {
                var profileName = propertyJson.get("profileName").getAsString();
                return new SkinValue("mojang", profileName, null, property);
            }
            
            var skinTexture = textures.getAsJsonObject("SKIN");
            var url = skinTexture.get("url").getAsString();
            
            var variant = SkinVariant.CLASSIC;
            var metadata = skinTexture.getAsJsonObject("metadata");
            if (metadata != null) {
                var model = metadata.get("model");
                if (model != null && "slim".equals(model.getAsString()))
                    variant = SkinVariant.SLIM;
            }
            
            return new SkinValue("web", url, variant, property);
        } catch (Exception e) {
            return SkinValue.EMPTY;
        }
    }
}
