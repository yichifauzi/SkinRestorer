package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.provider.EmptySkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SkinValue(@NotNull String provider, @Nullable String argument, @Nullable SkinVariant variant,
                        @Nullable Property value, @Nullable Property originalValue) {
    
    public static final SkinValue EMPTY = new SkinValue(EmptySkinProvider.PROVIDER_NAME, null, null, null);
    
    public SkinValue(String provider, String argument, SkinVariant variant, Property value) {
        this(provider, argument, variant, value, null);
    }
    
    public static SkinValue fromProviderContextWithValue(SkinProviderContext context, Property value) {
        return new SkinValue(context.name(), context.argument(), context.variant(), value);
    }
    
    public SkinProviderContext toProviderContext() {
        return new SkinProviderContext(this.provider, this.argument, this.variant);
    }
    
    public SkinValue replaceValueWithOriginal() {
        return new SkinValue(this.provider, this.argument, this.variant, this.originalValue, this.originalValue);
    }
    
    public SkinValue setOriginalValue(Property originalValue) {
        return new SkinValue(this.provider, this.argument, this.variant, this.value, originalValue);
    }
}
