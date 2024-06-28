package net.lionarius.skinrestorer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class SkinRestorer {
    public static final String MOD_ID = "skinrestorer";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");
    
    private static SkinStorage skinStorage;
    private static Path configDir;
    
    public static SkinStorage getSkinStorage() {
        return skinStorage;
    }
    
    public static Path getConfigDir() {
        return configDir;
    }
    
    public static void onInitialize(Path rootConfigDir) {
        SkinRestorer.configDir = rootConfigDir.resolve(MOD_ID);
        SkinRestorer.skinStorage = new SkinStorage(new SkinIO(SkinRestorer.configDir));
    }
    
    public static CompletableFuture<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>> setSkinAsync(MinecraftServer server, Collection<GameProfile> targets, Supplier<SkinResult> skinSupplier) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
                    SkinResult result = skinSupplier.get();
                    if (result.isError()) {
                        SkinRestorer.LOGGER.error("Could not get skin", result.getError());
                        return Pair.of(null, Collections.emptySet());
                    }
                    
                    Property skin = result.getSkin();
                    
                    for (GameProfile profile : targets) {
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                    }
                    
                    HashSet<GameProfile> acceptedProfiles = new HashSet<>(targets);
                    
                    return Pair.of(skin, acceptedProfiles);
                }).<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>>thenApplyAsync(pair -> {
                    Property skin = pair.left(); // NullPtrException will be caught by 'exceptionally'
                    
                    Collection<GameProfile> acceptedProfiles = pair.right();
                    HashSet<ServerPlayerEntity> acceptedPlayers = new HashSet<>();
                    
                    for (GameProfile profile : acceptedProfiles) {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(profile.getId());
                        
                        if (player == null || PlayerUtils.areSkinPropertiesEquals(skin, PlayerUtils.getPlayerSkin(player)))
                            continue;
                        
                        PlayerUtils.applyRestoredSkin(player, skin);
                        PlayerUtils.refreshPlayer(player);
                        acceptedPlayers.add(player);
                    }
                    return Pair.of(acceptedPlayers, acceptedProfiles);
                }, server)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    SkinRestorer.LOGGER.error(String.valueOf(e));
                    return Pair.of(Collections.emptySet(), Collections.emptySet());
                });
    }
}
