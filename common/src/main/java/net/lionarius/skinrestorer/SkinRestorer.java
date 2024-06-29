package net.lionarius.skinrestorer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class SkinRestorer {
    public static final String MOD_ID = "skinrestorer";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");
    
    private static final Map<String, SkinProvider> providers = new HashMap<>();
    private static SkinStorage skinStorage;
    private static Path configDir;
    
    private SkinRestorer() {}
    
    public static SkinStorage getSkinStorage() {
        return SkinRestorer.skinStorage;
    }
    
    public static Path getConfigDir() {
        return SkinRestorer.configDir;
    }
    
    public static Iterable<Map.Entry<String, SkinProvider>> getProviders() {
        return SkinRestorer.providers.entrySet();
    }
    
    public static Optional<SkinProvider> getProvider(String name) {
        return Optional.ofNullable(SkinRestorer.providers.get(name));
    }
    
    public static void onInitialize(Path rootConfigDir) {
        SkinRestorer.configDir = rootConfigDir.resolve(SkinRestorer.MOD_ID);
        
        SkinRestorer.providers.put("mojang", new MojangSkinProvider());
        SkinRestorer.providers.put("web", new MineskinSkinProvider());
    }
    
    public static void onServerStarted(MinecraftServer server) {
        Path worldSkinDirectory = server.getWorldPath(LevelResource.ROOT).resolve(SkinRestorer.MOD_ID);
        FileUtils.tryMigrateOldSkinDirectory(worldSkinDirectory);
        
        SkinRestorer.skinStorage = new SkinStorage(new SkinIO(worldSkinDirectory));
    }
    
    public static CompletableFuture<Pair<Collection<ServerPlayer>, Collection<GameProfile>>> setSkinAsync(
            MinecraftServer server,
            Collection<GameProfile> targets,
            Supplier<Result<Optional<Property>, ?>> skinSupplier
    ) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
                    var result = skinSupplier.get();
                    if (result.isError()) {
                        SkinRestorer.LOGGER.error("Could not get skin: {}", result.getErrorValue());
                        return Pair.of(null, Collections.emptySet());
                    }
                    
                    Property skin = result.getSuccessValue().orElse(null);
                    
                    for (GameProfile profile : targets) {
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                    }
                    
                    HashSet<GameProfile> acceptedProfiles = new HashSet<>(targets);
                    
                    return Pair.of(skin, acceptedProfiles);
                }).<Pair<Collection<ServerPlayer>, Collection<GameProfile>>>thenApplyAsync(pair -> {
                    Property skin = pair.left(); // NullPtrException will be caught by 'exceptionally'
                    
                    Collection<GameProfile> acceptedProfiles = pair.right();
                    HashSet<ServerPlayer> acceptedPlayers = new HashSet<>();
                    
                    for (GameProfile profile : acceptedProfiles) {
                        ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
                        
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
