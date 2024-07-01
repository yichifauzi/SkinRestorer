package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SkinValue(@NotNull String provider, @Nullable String argument, @Nullable SkinVariant variant,
                        @Nullable Property value) {
    
    public static final SkinValue EMPTY = new SkinValue("empty", null, null, null);
    
    public static SkinValue fromProviderContextWithValue(SkinProviderContext context, Property value) {
        return new SkinValue(context.name(), context.argument(), context.variant(), value);
    }
    
    public SkinProviderContext toProviderContext() {
        return new SkinProviderContext(this.provider, this.argument, this.variant);
    }
}
