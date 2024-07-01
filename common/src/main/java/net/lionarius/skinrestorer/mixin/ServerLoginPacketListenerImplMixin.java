package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
    @Shadow @Final
    MinecraftServer server;
    
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
                
                if (SkinRestorer.getSkinStorage().hasSavedSkin(authenticatedProfile.getId()))
                    return null;
                
                if (!server.enforceSecureProfile() && SkinRestorer.getConfig().fetchSkinOnFirstJoin()) { // fetch from mojang provider by username
                    var context = new SkinProviderContext("mojang", authenticatedProfile.getName(), null);
                    var result = SkinRestorer.getProvider(context.name()).map(
                            provider -> provider.getSkin(context.argument(), context.variant())
                    ).orElse(Result.ofNullable(null));
                    
                    if (!result.isError()) {
                        var value = SkinValue.fromProviderContextWithValue(context, result.getSuccessValue().orElse(null));
                        SkinRestorer.getSkinStorage().setSkin(authenticatedProfile.getId(), value);
                    } else {
                        SkinRestorer.LOGGER.error("failed to fetch skin on first join", result.getErrorValue());
                    }
                }
                
                return null;
            });
        }
        
        if (!skinrestorer_pendingSkin.isDone())
            ci.cancel();
    }
}
