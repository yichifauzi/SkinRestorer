package net.lionarius.skinrestorer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
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
        player.networkHandler.sendPacket(
                new PlayerRespawnS2CPacket(player.createCommonPlayerSpawnInfo(serverWorld), PlayerRespawnS2CPacket.KEEP_ALL)
        );
        player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.sendAbilitiesUpdate();
        player.addExperience(0);
        playerManager.sendCommandTree(player);
        playerManager.sendWorldInfo(player, serverWorld);
        playerManager.sendPlayerStatus(player);
        for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
            player.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), statusEffectInstance));
        }
    }

    public static CompletableFuture<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>> setSkinAsync(MinecraftServer server, Collection<GameProfile> targets, Supplier<Property> skinSupplier) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
            HashSet<GameProfile> acceptedProfiles = new HashSet<>();
            Property skin = skinSupplier.get();
            if (Objects.isNull(skin)) {
                SkinRestorer.LOGGER.error("Cannot get the skin for {}", targets.stream().findFirst().orElseThrow());
                return Pair.of(null, Collections.emptySet());
            }

            for (GameProfile profile : targets) {
                SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                acceptedProfiles.add(profile);
            }

            return Pair.of(skin, acceptedProfiles);
        }).<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>>thenApplyAsync(pair -> {
            Property skin = pair.left();
            if (Objects.isNull(skin))
                return Pair.of(Collections.emptySet(), Collections.emptySet());

            Collection<GameProfile> acceptedProfiles = pair.right();
            HashSet<ServerPlayerEntity> acceptedPlayers = new HashSet<>();
            JsonObject newSkinJson = gson.fromJson(new String(Base64.getDecoder().decode(skin.value()), StandardCharsets.UTF_8), JsonObject.class);
            newSkinJson.remove("timestamp");
            for (GameProfile profile : acceptedProfiles) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(profile.getId());

                if (player == null || arePropertiesEquals(newSkinJson, player.getGameProfile()))
                    continue;

                applyRestoredSkin(player.getGameProfile(), skin);
                refreshPlayer(player);
                acceptedPlayers.add(player);
            }
            return Pair.of(acceptedPlayers, acceptedProfiles);
        }, server).orTimeout(10, TimeUnit.SECONDS).exceptionally(e -> Pair.of(Collections.emptySet(), Collections.emptySet()));
    }

    public static void applyRestoredSkin(GameProfile profile, Property skin) {
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", skin);
    }

    private static final Gson gson = new Gson();

    private static boolean arePropertiesEquals(@NotNull JsonObject x, @NotNull GameProfile y) {
        Property py = y.getProperties().get("textures").stream().findFirst().orElse(null);
        if (py == null)
            return false;

        try {
            JsonObject jy = gson.fromJson(new String(Base64.getDecoder().decode(py.value()), StandardCharsets.UTF_8), JsonObject.class);
            jy.remove("timestamp");
            return x.equals(jy);
        } catch (Exception ex) {
            SkinRestorer.LOGGER.info("Can not compare skin", ex);
            return false;
        }
    }
}
