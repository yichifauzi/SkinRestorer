package net.lionarius.skinrestorer.skin.provider;

import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.skin.SkinVariant;

public interface SkinProvider {
    
    String getArgumentName();
    
    boolean hasVariantSupport();
    
    SkinResult getSkin(String argument, SkinVariant variant);
}
