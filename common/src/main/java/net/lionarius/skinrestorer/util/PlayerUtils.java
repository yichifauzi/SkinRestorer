package net.lionarius.skinrestorer.util;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import java.util.Collections;
import java.util.List;

public class PlayerUtils {
    
    public static final String TEXTURES_KEY = "textures";
    
    public static boolean isFakePlayer(ServerPlayerEntity player) {
        return player.getClass() != ServerPlayerEntity.class; // if the player isn't a server player entity, it must be someone's fake player
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
        
        if (!player.isDead()) {
            chunkManager.unloadEntity(player);
            chunkManager.loadEntity(player);
            player.networkHandler.send(new BundleS2CPacket(
                    List.of(
                            new PlayerRespawnS2CPacket(player.createCommonPlayerSpawnInfo(serverWorld), PlayerRespawnS2CPacket.KEEP_ALL),
                            new GameStateChangeS2CPacket(GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING, 0)
                    )
            ), null);
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            player.networkHandler.send(new EntityVelocityUpdateS2CPacket(player), null);
            player.sendAbilitiesUpdate();
            player.addExperience(0);
            playerManager.sendCommandTree(player);
            playerManager.sendWorldInfo(player, serverWorld);
            playerManager.sendPlayerStatus(player);
            playerManager.sendStatusEffects(player);
        }
    }
    
    public static Property getPlayerSkin(ServerPlayerEntity player) {
        return player.getGameProfile().getProperties().get(TEXTURES_KEY).stream().findFirst().orElse(null);
    }
    
    public static void applyRestoredSkin(ServerPlayerEntity player, Property skin) {
        GameProfile profile = player.getGameProfile();
        profile.getProperties().removeAll(TEXTURES_KEY);
        
        if (skin != null)
            profile.getProperties().put(TEXTURES_KEY, skin);
    }
    
    
    public static boolean areSkinPropertiesEquals(Property x, Property y) {
        if (x == y)
            return true;
        
        if (x == null || y == null)
            return false;
        
        if (x.equals(y))
            return true;
        
        JsonObject xJson = JsonUtils.skinPropertyToJson(x);
        JsonObject yJson = JsonUtils.skinPropertyToJson(y);
        
        if (xJson == null || yJson == null)
            return false;
        
        return xJson.equals(yJson);
    }
}
