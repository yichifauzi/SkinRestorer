package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Either;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public final class MineskinSkinProvider implements SkinProvider {
    
    private static final URI API_URL;
    
    static {
        try {
            API_URL = new URI("https://api.mineskin.org/generate/url");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    public String getArgumentName() {
        return "url";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return true;
    }
    
    @Override
    public Result<Optional<Property>, Exception> getSkin(String url, SkinVariant variant) {
        try {
            String body = ("{\"variant\":\"%s\",\"name\":\"%s\",\"visibility\":%d,\"url\":\"%s\"}")
                    .formatted(variant.toString(), "none", 1, url);
            
            JsonObject texture = JsonUtils.parseJson(WebUtils.postRequest(API_URL.toURL(), "application/json", body))
                    .getAsJsonObject("data").getAsJsonObject("texture");
            
            return Result.ofNullable(new Property(PlayerUtils.TEXTURES_KEY, texture.get("value").getAsString(), texture.get("signature").getAsString()));
        } catch (Exception e) {
            return Result.error(e);
        }
    }
}
