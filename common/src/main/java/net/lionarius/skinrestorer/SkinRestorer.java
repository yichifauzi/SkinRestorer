package net.lionarius.skinrestorer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderRegistry;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.Translation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class SkinRestorer {
    public static final String MOD_ID = "skinrestorer";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");
    
    private static final SkinProviderRegistry providersRegistry = new SkinProviderRegistry();
    private static SkinStorage skinStorage;
    private static Path configDir;
    private static Config config;
    
    private SkinRestorer() {}
    
    public static SkinStorage getSkinStorage() {
        return SkinRestorer.skinStorage;
    }
    
    public static Path getConfigDir() {
        return SkinRestorer.configDir;
    }
    
    public static Config getConfig() {
        return SkinRestorer.config;
    }
    
    public static SkinProviderRegistry getProvidersRegistry() {
        return SkinRestorer.providersRegistry;
    }
    
    public static Optional<SkinProvider> getProvider(String name) {
        return Optional.ofNullable(SkinRestorer.providersRegistry.get(name));
    }
    
    public static void onInitialize(Path rootConfigDir) {
        SkinRestorer.configDir = rootConfigDir.resolve(SkinRestorer.MOD_ID);
        
        SkinRestorer.providersRegistry.register("mojang", SkinProvider.MOJANG);
        SkinRestorer.providersRegistry.register("web", SkinProvider.MINESKIN);
    }
    
    public static void onServerStarted(MinecraftServer server) {
        Path worldSkinDirectory = server.getWorldPath(LevelResource.ROOT).resolve(SkinRestorer.MOD_ID);
        FileUtils.tryMigrateOldSkinDirectory(worldSkinDirectory);
        
        SkinRestorer.reloadConfig();
        SkinRestorer.skinStorage = new SkinStorage(new SkinIO(worldSkinDirectory));
    }
    
    public static void reloadConfig() {
        SkinRestorer.config = Config.load(SkinRestorer.getConfigDir());
        Translation.reloadTranslations();
    }
    
    public static String resource(String name) {
        return String.format("/assets/%s/%s", SkinRestorer.MOD_ID, name);
    }
    
    public static CompletableFuture<Result<Collection<ServerPlayer>, String>> setSkinAsync(
            MinecraftServer server,
            Collection<GameProfile> targets,
            Supplier<Result<Optional<Property>, Exception>> skinSupplier
    ) {
        return CompletableFuture.supplyAsync(skinSupplier)
                .thenApplyAsync(skinResult -> {
                    if (skinResult.isError()) {
                        return Result.<Collection<ServerPlayer>, String>error(skinResult.getErrorValue().getMessage());
                    }
                    
                    var skin = skinResult.getSuccessValue().orElse(null);
                    HashSet<ServerPlayer> acceptedPlayers = new HashSet<>();
                    
                    for (GameProfile profile : targets) {
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                        ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
                        
                        if (player == null || PlayerUtils.areSkinPropertiesEquals(skin, PlayerUtils.getPlayerSkin(player)))
                            continue;
                        
                        PlayerUtils.applyRestoredSkin(player, skin);
                        PlayerUtils.refreshPlayer(player);
                        acceptedPlayers.add(player);
                    }
                    return Result.<Collection<ServerPlayer>, String>success(acceptedPlayers);
                }, server)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    var cause = String.valueOf(e);
                    SkinRestorer.LOGGER.error(cause);
                    return Result.error(cause);
                });
    }
}
