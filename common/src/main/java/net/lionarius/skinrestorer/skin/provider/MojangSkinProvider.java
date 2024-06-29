package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

public final class MojangSkinProvider implements SkinProvider {
    
    private static final URI API_URL;
    private static final URI SESSION_SERVER_URL;
    
    static {
        try {
            API_URL = new URI("https://api.mojang.com/users/profiles/minecraft/");
            SESSION_SERVER_URL = new URI("https://sessionserver.mojang.com/session/minecraft/profile/");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    
    @Override
    public String getArgumentName() {
        return "username";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> getSkin(String username, SkinVariant variant) {
        try {
            UUID uuid = getUUID(username);
            JsonObject texture = JsonUtils.parseJson(WebUtils.getRequest(SESSION_SERVER_URL.resolve(uuid + "?unsigned=false").toURL()))
                    .getAsJsonArray("properties").get(0).getAsJsonObject();
            
            return Result.ofNullable(new Property(PlayerUtils.TEXTURES_KEY, texture.get("value").getAsString(), texture.get("signature").getAsString()));
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static UUID getUUID(String name) throws IOException {
        return UUID.fromString(JsonUtils.parseJson(WebUtils.getRequest(API_URL.resolve(name).toURL())).get("id").getAsString()
                .replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }
}
