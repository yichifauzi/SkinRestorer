package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    
    @Shadow @Nullable
    private GameProfile authenticatedProfile;
    
    @Unique
    private CompletableFuture<Void> skinrestorer_pendingSkin;
    
    @Inject(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE",
                                                                     target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"),
            cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer_pendingSkin == null) {
            skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
                assert authenticatedProfile != null;
                SkinRestorer.LOGGER.debug("Fetching {}'s skin", authenticatedProfile.getName());
                
                if (!SkinRestorer.getSkinStorage().hasSavedSkin(authenticatedProfile.getId())) { // when player joins for the first time fetch Mojang skin by his username
                    var result = SkinRestorer.getProvider("mojang").map(
                            provider -> provider.getSkin(authenticatedProfile.getName(), SkinVariant.CLASSIC)
                    ).orElse(Result.ofNullable(null));
                    
                    if (!result.isError())
                        SkinRestorer.getSkinStorage().setSkin(authenticatedProfile.getId(), result.getSuccessValue().orElse(null));
                }
                
                SkinRestorer.getSkinStorage().getSkin(authenticatedProfile.getId()); // loads skin from disk
                
                return null;
            });
        }
        
        if (!skinrestorer_pendingSkin.isDone())
            ci.cancel();
    }
}
