package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;

public final class EmptySkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "empty";
    
    @Override
    public String getArgumentName() {
        return "placeholder";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> getSkin(String argument, SkinVariant variant) {
        return this.getSkin();
    }
    
    public Result<Optional<Property>, Exception> getSkin() {
        return Result.ofNullable(null);
    }
}
