package net.lionarius.skinrestorer;

import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SkinResult {
    private final Property skin;
    private final boolean isError;

    private SkinResult(Property skin, boolean isError) {
        this.skin = skin;
        this.isError = isError;
    }

    public Optional<Property> getSkin() {
        return Optional.ofNullable(this.skin);
    }

    public boolean isError() {
        return this.isError;
    }

    public static SkinResult empty() {
        return new SkinResult(null, false);
    }

    public static SkinResult error() {
        return new SkinResult(null, true);
    }

    public static SkinResult success(@NotNull Property skin) {
        return new SkinResult(skin, false);
    }

    public static SkinResult ofNullable(Property skin) {
        if (skin == null)
            return SkinResult.empty();

        return SkinResult.success(skin);
    }
}
