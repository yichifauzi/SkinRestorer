package net.lionarius.skinrestorer.util;

import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerUtils {

    public static boolean isFakePlayer(ServerPlayerEntity player) {
        return player.getClass() != ServerPlayerEntity.class; // if the player isn't a server player entity, it must be someone's fake player
    }
}
