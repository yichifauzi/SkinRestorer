package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
    public SkinResult getSkin(String url, SkinVariant variant) {
        try {
            String body = ("{\"variant\":\"%s\",\"name\":\"%s\",\"visibility\":%d,\"url\":\"%s\"}")
                    .formatted(variant.toString(), "none", 1, url);
            
            JsonObject texture = JsonUtils.parseJson(WebUtils.postRequest(API_URL.toURL(), "application/json", body))
                    .getAsJsonObject("data").getAsJsonObject("texture");
            
            return SkinResult.success(new Property(PlayerUtils.TEXTURES_KEY, texture.get("value").getAsString(), texture.get("signature").getAsString()));
        } catch (IOException e) {
            return SkinResult.error(e);
        }
    }
}
