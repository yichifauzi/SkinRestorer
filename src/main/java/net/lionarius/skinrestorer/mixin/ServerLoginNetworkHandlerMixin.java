package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.SkinResult;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow @Nullable
    private GameProfile profile;

    @Unique
    private CompletableFuture<SkinResult> skinrestorer_pendingSkin;

    @Inject(method = "tickVerify", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"), cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer_pendingSkin == null) {
            skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
                SkinRestorer.LOGGER.debug("Fetching {}'s skin", profile.getName());

                if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) { // when player joins for the first time fetch Mojang skin by his username
                    SkinResult result = MojangSkinProvider.getSkin(profile.getName());
                    if (!result.isError())
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), result.getSkin().orElse(null));
                }

                return SkinResult.ofNullable(SkinRestorer.getSkinStorage().getSkin(profile.getId()));
            });
        }

        if (!skinrestorer_pendingSkin.isDone()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendSuccessPacket", at = @At("HEAD"))
    public void applyRestoredSkinHook(GameProfile profile, CallbackInfo ci) {
        if (skinrestorer_pendingSkin != null)
            SkinRestorer.applyRestoredSkin(profile, skinrestorer_pendingSkin.getNow(SkinResult.empty()).getSkin().orElse(null));
    }
}
