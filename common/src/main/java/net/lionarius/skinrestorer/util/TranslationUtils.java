package net.lionarius.skinrestorer.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TranslationUtils {
    public static final String TRANSLATION_FILENAME = "translation";
    
    public static final String COMMAND_SKIN_AFFECTED_PLAYERS_KEY = "skinrestorer.command.skin.affected_players";
    public static final String COMMAND_SKIN_FAILED_KEY = "skinrestorer.command.skin.failed";
    public static final String COMMAND_SKIN_OK_KEY = "skinrestorer.command.skin.ok";
    
    private static Map<String, String> translations;
    
    private TranslationUtils() {}
    
    public static String getTranslation(String key) {
        return TranslationUtils.translations.get(key);
    }
    
    public static MutableComponent translatableWithFallback(String key) {
        return Component.translatableWithFallback(key, TranslationUtils.getTranslation(key));
    }
    
    public static MutableComponent translatableWithFallback(String key, Object... args) {
        return Component.translatableWithFallback(key, TranslationUtils.getTranslation(key), args);
    }
    
    static {
        Path path = SkinRestorer.getConfigDir().resolve(TRANSLATION_FILENAME + SkinIO.FILE_EXTENSION);
        translations = null;
        
        if (Files.exists(path)) {
            try {
                var mapType = new TypeToken<HashMap<String, String>>() {}.getType();
                translations = JsonUtils.fromJson(Objects.requireNonNull(FileUtils.readFile(path.toFile())), mapType);
            } catch (Exception ex) {
                SkinRestorer.LOGGER.error("Failed to load translation", ex);
            }
        }
        
        if (translations == null) {
            translations = ImmutableMap.<String, String>builder()
                    .put(COMMAND_SKIN_AFFECTED_PLAYERS_KEY, "Applied skin changes for %s")
                    .put(COMMAND_SKIN_FAILED_KEY, "Failed to set skin: %s")
                    .put(COMMAND_SKIN_OK_KEY, "Skin changed")
                    .build();
        }
    }
}
