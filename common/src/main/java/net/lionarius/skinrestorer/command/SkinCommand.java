package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.TranslationUtils;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkinCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("skin")
                .then(literal("set")
                        .then(literal("mojang")
                                .then(argument("skin_name", StringArgumentType.word())
                                        .executes(context ->
                                                skinAction(context.getSource(),
                                                        () -> MojangSkinProvider.getSkin(StringArgumentType.getString(context, "skin_name"))))
                                        .then(argument("targets", GameProfileArgumentType.gameProfile()).requires(source -> source.hasPermissionLevel(2))
                                                .executes(context ->
                                                        skinAction(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), true,
                                                                () -> MojangSkinProvider.getSkin(StringArgumentType.getString(context, "skin_name")))))))
                        .then(literal("web")
                                .then(literal("classic")
                                        .then(argument("url", StringArgumentType.string())
                                                .executes(context ->
                                                        skinAction(context.getSource(),
                                                                () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.CLASSIC)))
                                                .then(argument("targets", GameProfileArgumentType.gameProfile()).requires(source -> source.hasPermissionLevel(2))
                                                        .executes(context ->
                                                                skinAction(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), true,
                                                                        () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.CLASSIC))))))
                                .then(literal("slim")
                                        .then(argument("url", StringArgumentType.string())
                                                .executes(context ->
                                                        skinAction(context.getSource(),
                                                                () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.SLIM)))
                                                .then(argument("targets", GameProfileArgumentType.gameProfile()).requires(source -> source.hasPermissionLevel(2))
                                                        .executes(context ->
                                                                skinAction(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), true,
                                                                        () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.SLIM))))))))
                .then(literal("clear")
                        .executes(context ->
                                skinAction(context.getSource(),
                                        SkinResult::empty))
                        .then(argument("targets", GameProfileArgumentType.gameProfile()).requires(source -> source.hasPermissionLevel(2)).executes(context ->
                                skinAction(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), true,
                                        SkinResult::empty))))
        );
    }
    
    private static int skinAction(ServerCommandSource src, Collection<GameProfile> targets, boolean setByOperator, Supplier<SkinResult> skinSupplier) {
        SkinRestorer.setSkinAsync(src.getServer(), targets, skinSupplier).thenAccept(pair -> {
            Collection<GameProfile> profiles = pair.right();
            Collection<ServerPlayerEntity> players = pair.left();
            
            if (profiles.isEmpty()) {
                src.sendError(Text.of(TranslationUtils.translation.skinActionFailed));
                return;
            }
            
            if (setByOperator) {
                src.sendFeedback(() -> Text.of(
                        String.format(TranslationUtils.translation.skinActionAffectedProfile,
                                String.join(", ", profiles.stream().map(GameProfile::getName).toList()))), true);
                
                if (!players.isEmpty()) {
                    src.sendFeedback(() -> Text.of(
                            String.format(TranslationUtils.translation.skinActionAffectedPlayer,
                                    String.join(", ", players.stream().map(p -> p.getGameProfile().getName()).toList()))), true);
                }
            } else {
                src.sendFeedback(() -> Text.of(TranslationUtils.translation.skinActionOk), true);
            }
        });
        return targets.size();
    }
    
    private static int skinAction(ServerCommandSource src, Supplier<SkinResult> skinSupplier) {
        if (src.getPlayer() == null)
            return 0;
        
        return skinAction(src, Collections.singleton(src.getPlayer().getGameProfile()), false, skinSupplier);
    }
}
