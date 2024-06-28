package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TranslationUtils {
    public static class Translation {
        public String skinActionAffectedProfile = "Skin has been saved for %s";
        public String skinActionAffectedPlayer = "Apply live skin changes for %s";
        public String skinActionFailed = "Failed to set skin";
        public String skinActionOk = "Skin changed";
    }
    
    public static final String TRANSLATION_FILENAME = "translation";
    
    static {
        Path path = SkinRestorer.getConfigDir().resolve(TRANSLATION_FILENAME + SkinIO.FILE_EXTENSION);
        
        if (Files.exists(path)) {
            try {
                translation = JsonUtils.fromJson(Objects.requireNonNull(FileUtils.readFile(path.toFile())), Translation.class);
            } catch (Exception ex) {
                SkinRestorer.LOGGER.error("Failed to load translation", ex);
            }
        }
    }
}
