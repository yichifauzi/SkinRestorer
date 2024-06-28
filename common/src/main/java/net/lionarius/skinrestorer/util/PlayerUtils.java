package net.lionarius.skinrestorer.util;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Collections;
import java.util.List;

public final class PlayerUtils {
    
    public static final String TEXTURES_KEY = "textures";
    
    private PlayerUtils() {}
    
    public static boolean isFakePlayer(ServerPlayer player) {
        return player.getClass() != ServerPlayer.class; // if the player isn't a server player entity, it must be someone's fake player
    }
    
    public static void refreshPlayer(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        PlayerList playerList = serverLevel.getServer().getPlayerList();
        ServerChunkCache chunkSource = serverLevel.getChunkSource();
        
        playerList.broadcastAll(new ClientboundBundlePacket(
                List.of(
                        new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())),
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(player))
                )
        ));
        
        if (!player.isDeadOrDying()) {
            chunkSource.removeEntity(player);
            chunkSource.addEntity(player);
            player.connection.send(new ClientboundBundlePacket(
                    List.of(
                            new ClientboundRespawnPacket(player.createCommonSpawnInfo(serverLevel), ClientboundRespawnPacket.KEEP_ALL_DATA),
                            new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0)
                    )
            ));
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            player.onUpdateAbilities();
            player.giveExperiencePoints(0);
            playerList.sendPlayerPermissionLevel(player);
            playerList.sendLevelInfo(player, serverLevel);
            playerList.sendAllPlayerInfo(player);
            playerList.sendActivePlayerEffects(player);
        }
    }
    
    public static Property getPlayerSkin(ServerPlayer player) {
        return player.getGameProfile().getProperties().get(TEXTURES_KEY).stream().findFirst().orElse(null);
    }
    
    public static void applyRestoredSkin(ServerPlayer player, Property skin) {
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
