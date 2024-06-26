package net.lionarius.skinrestorer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SkinRestorer implements DedicatedServerModInitializer {

    private static SkinStorage skinStorage;

    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");

    public static SkinStorage getSkinStorage() {
        return skinStorage;
    }

    @Override
    public void onInitializeServer() {
        skinStorage = new SkinStorage(new SkinIO(FabricLoader.getInstance().getConfigDir().resolve("skinrestorer")));
    }

    public static void refreshPlayer(ServerPlayerEntity player) {
        ServerWorld serverWorld = player.getServerWorld();
        PlayerManager playerManager = serverWorld.getServer().getPlayerManager();
        ServerChunkManager chunkManager = serverWorld.getChunkManager();

        playerManager.sendToAll(new BundleS2CPacket(
                List.of(
                        new PlayerRemoveS2CPacket(List.of(player.getUuid())),
                        PlayerListS2CPacket.entryFromPlayer(Collections.singleton(player))
                )
        ));
        chunkManager.unloadEntity(player);
        chunkManager.loadEntity(player);
        player.networkHandler.sendPacket(new BundleS2CPacket(
                List.of(
                        new PlayerRespawnS2CPacket(player.createCommonPlayerSpawnInfo(serverWorld), PlayerRespawnS2CPacket.KEEP_ALL),
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING, 0)
                )
        ));
        player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.sendAbilitiesUpdate();
        player.addExperience(0);
        playerManager.sendCommandTree(player);
        playerManager.sendWorldInfo(player, serverWorld);
        playerManager.sendPlayerStatus(player);
        playerManager.sendStatusEffects(player);
    }

    public static CompletableFuture<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>> setSkinAsync(MinecraftServer server, Collection<GameProfile> targets, Supplier<SkinResult> skinSupplier) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
                    SkinResult result = skinSupplier.get();
                    if (result.isError()) {
                        SkinRestorer.LOGGER.error("Could not get skin", result.getError());
                        return Pair.of(null, Collections.emptySet());
                    }

                    Property skin = result.getSkin();

                    for (GameProfile profile : targets)
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);

                    HashSet<GameProfile> acceptedProfiles = new HashSet<>(targets);

                    return Pair.of(skin, acceptedProfiles);
                }).<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>>thenApplyAsync(pair -> {
                    Property skin = pair.left(); // NullPtrException will be caught by 'exceptionally'

                    Collection<GameProfile> acceptedProfiles = pair.right();
                    HashSet<ServerPlayerEntity> acceptedPlayers = new HashSet<>();

                    for (GameProfile profile : acceptedProfiles) {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(profile.getId());

                        if (player == null || areSkinPropertiesEquals(skin, getPlayerSkin(player)))
                            continue;

                        applyRestoredSkin(player.getGameProfile(), skin);
                        refreshPlayer(player);
                        acceptedPlayers.add(player);
                    }
                    return Pair.of(acceptedPlayers, acceptedProfiles);
                }, server)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> Pair.of(Collections.emptySet(), Collections.emptySet()));
    }

    public static void applyRestoredSkin(GameProfile profile, Property skin) {
        profile.getProperties().removeAll("textures");
        if (skin != null)
            profile.getProperties().put("textures", skin);
    }

    private static Property getPlayerSkin(ServerPlayerEntity player) {
        return player.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
    }

    private static final Gson gson = new Gson();

    private static JsonObject skinPropertyToJson(Property property) {
        try {
            JsonObject json = gson.fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonObject.class);
            if (!Objects.isNull(json))
                json.remove("timestamp");

            return json;
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean areSkinPropertiesEquals(Property x, Property y) {
        if (x == y)
            return true;

        if (x == null || y == null)
            return false;

        if (x.equals(y))
            return true;

        JsonObject xJson = skinPropertyToJson(x);
        JsonObject yJson = skinPropertyToJson(y);

        if (xJson == null || yJson == null)
            return false;

        return xJson.equals(yJson);
    }
}
