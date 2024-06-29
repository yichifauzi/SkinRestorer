package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;

public interface SkinProvider {
    
    String getArgumentName();
    
    boolean hasVariantSupport();
    
    Result<Optional<Property>, Exception> getSkin(String argument, SkinVariant variant);
}
