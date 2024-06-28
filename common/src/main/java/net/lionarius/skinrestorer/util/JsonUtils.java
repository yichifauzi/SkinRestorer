package net.lionarius.skinrestorer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private JsonUtils() {}
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    public static JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
    
    public static JsonObject skinPropertyToJson(Property property) {
        try {
            JsonObject json = GSON.fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonObject.class);
            if (json != null)
                json.remove("timestamp");
            
            return json;
        } catch (Exception ex) {
            return null;
        }
    }
}
