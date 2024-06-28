package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinIO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtils {
    
    public static void tryMigrateOldSkinDirectory(Path newDirectory) {
        try {
            File newDirectoryFile = newDirectory.toFile();
            if (!newDirectoryFile.exists())
                newDirectoryFile.mkdirs();
            
            File configDirectory = SkinRestorer.getConfigDir().toFile();
            File[] files = configDirectory.listFiles(
                    (file, name) -> !name.startsWith(TranslationUtils.TRANSLATION_FILENAME) && name.endsWith(SkinIO.FILE_EXTENSION)
            );
            if (files == null)
                return;
            
            for (File file : files) {
                if (file.isFile())
                    Files.move(file.toPath(), newDirectory.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Could not migrate skin directory", e);
        }
    }
    
    public static String readFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            return StringUtils.readString(reader);
        } catch (IOException e) {
            return null;
        }
    }
    
    public static boolean writeFile(File path, String fileName, String content) {
        try {
            if (!path.exists())
                path.mkdirs();
            
            File file = new File(path, fileName);
            if (!file.exists())
                file.createNewFile();
            
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
