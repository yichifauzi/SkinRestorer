package net.lionarius.skinrestorer.util;

import com.google.common.reflect.TypeToken;
import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Translation {
    public static final String LEGACY_TRANSLATION_FILENAME = "translation";
    
    public static final String COMMAND_SKIN_AFFECTED_PLAYERS_KEY = "skinrestorer.command.skin.affected_players";
    public static final String COMMAND_SKIN_FAILED_KEY = "skinrestorer.command.skin.failed";
    public static final String COMMAND_SKIN_OK_KEY = "skinrestorer.command.skin.ok";
    
    private static Map<String, String> translations;
    
    private Translation() {}
    
    public static String get(String key) {
        return translations.get(key);
    }
    
    public static MutableComponent translatableWithFallback(String key) {
        return Component.translatableWithFallback(key, Translation.get(key));
    }
    
    public static MutableComponent translatableWithFallback(String key, Object... args) {
        return Component.translatableWithFallback(key, Translation.get(key), args);
    }
    
    static {
        try {
            translations = Translation.loadTranslations("en_us");
        } catch (Exception ex) {
            SkinRestorer.LOGGER.error("Failed to load translation", ex);
        }
        
        if (translations == null) {
            try {
                translations = Translation.loadTranslations("en_us");
            } catch (Exception ex) {
                SkinRestorer.LOGGER.error("Failed to default translation", ex);
            }
        }
        
        if (translations == null)
            translations = new HashMap<>();
    }
    
    private static HashMap<String, String> loadTranslations(String lang) {
        var json = FileUtils.readResource(SkinRestorer.resource(String.format("lang/%s.json", lang)));
        
        var type = new TypeToken<HashMap<String, String>>() {}.getType();
        return JsonUtils.fromJson(Objects.requireNonNull(json), type);
    }
}
