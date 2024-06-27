package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class MojangSkinProvider {
    
    private static final String API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_SERVER = "https://sessionserver.mojang.com/session/minecraft/profile/";
    
    public static SkinResult getSkin(String name) {
        try {
            UUID uuid = getUUID(name);
            JsonObject texture = JsonUtils.parseJson(WebUtils.GETRequest(new URL(SESSION_SERVER + uuid + "?unsigned=false")))
                    .getAsJsonArray("properties").get(0).getAsJsonObject();
            
            return SkinResult.success(new Property(PlayerUtils.TEXTURES_KEY, texture.get("value").getAsString(), texture.get("signature").getAsString()));
        } catch (Exception e) {
            return SkinResult.error(e);
        }
    }
    
    private static UUID getUUID(String name) throws IOException {
        return UUID.fromString(JsonUtils.parseJson(WebUtils.GETRequest(new URL(API + name))).get("id").getAsString()
                .replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }
}
