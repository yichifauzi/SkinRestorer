package net.lionarius.skinrestorer;

import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.NotNull;

public class SkinResult {
    private final Property skin;
    private final Exception exception;

    private SkinResult(Property skin, Exception exception) {
        this.skin = skin;
        this.exception = exception;
    }

    public Property getSkin() {
        return this.skin;
    }

    public Exception getError() {
        return this.exception;
    }

    public boolean isError() {
        return this.exception != null;
    }

    public static SkinResult empty() {
        return new SkinResult(null, null);
    }

    public static SkinResult error(Exception e) {
        return new SkinResult(null, e);
    }

    public static SkinResult success(@NotNull Property skin) {
        return new SkinResult(skin, null);
    }

    public static SkinResult ofNullable(Property skin) {
        if (skin == null)
            return SkinResult.empty();

        return SkinResult.success(skin);
    }
}
