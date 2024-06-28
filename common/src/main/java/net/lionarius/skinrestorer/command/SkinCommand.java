package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinResult;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.TranslationUtils;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkinCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> base =
                literal("skin")
                        .then(buildAction("clear", SkinResult::empty));
        
        LiteralArgumentBuilder<ServerCommandSource> set = literal("set");
        
        for (Map.Entry<String, SkinProvider> entry : SkinRestorer.getProviders()) {
            set.then(buildAction(entry.getKey(), entry.getValue()));
        }
        
        base.then(set);
        
        dispatcher.register(base);
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildAction(String name, SkinProvider provider) {
        LiteralArgumentBuilder<ServerCommandSource> action = literal(name);
        
        if (provider.hasVariantSupport()) {
            for (SkinVariant variant : SkinVariant.values()) {
                action.then(
                        literal(variant.toString())
                                .then(buildArgument(
                                        argument(provider.getArgumentName(), StringArgumentType.string()),
                                        context -> provider.getSkin(StringArgumentType.getString(context, provider.getArgumentName()), variant)
                                ))
                );
            }
        } else {
            action.then(
                    buildArgument(
                            argument(provider.getArgumentName(), StringArgumentType.string()),
                            context -> provider.getSkin(StringArgumentType.getString(context, provider.getArgumentName()), SkinVariant.CLASSIC)
                    )
            );
        }
        
        return action;
    }
    
    private static ArgumentBuilder<ServerCommandSource, LiteralArgumentBuilder<ServerCommandSource>> buildAction(String name, Supplier<SkinResult> supplier) {
        return buildArgument(literal(name), context -> supplier.get());
    }
    
    private static <T extends ArgumentBuilder<ServerCommandSource, T>> ArgumentBuilder<ServerCommandSource, T> buildArgument(
            ArgumentBuilder<ServerCommandSource, T> argument,
            Function<CommandContext<ServerCommandSource>, SkinResult> provider
    ) {
        return argument
                .executes(context -> skinAction(
                        context.getSource(),
                        () -> provider.apply(context)
                ))
                .then(makeTargetsArgument(provider));
    }
    
    private static RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> makeTargetsArgument(
            Function<CommandContext<ServerCommandSource>, SkinResult> provider
    ) {
        return argument("targets", GameProfileArgumentType.gameProfile())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> skinAction(
                        context.getSource(),
                        GameProfileArgumentType.getProfileArgument(context, "targets"),
                        true,
                        () -> provider.apply(context)
                ));
    }
    
    private static int skinAction(ServerCommandSource src, Collection<GameProfile> targets, boolean setByOperator, Supplier<SkinResult> skinSupplier) {
        SkinRestorer.setSkinAsync(src.getServer(), targets, skinSupplier).thenAccept(pair -> {
            Collection<GameProfile> profiles = pair.right();
            Collection<ServerPlayerEntity> players = pair.left();
            
            if (profiles.isEmpty()) {
                src.sendError(Text.of(TranslationUtils.getTranslation().skinActionFailed));
                return;
            }
            
            if (setByOperator) {
                src.sendFeedback(() -> Text.of(
                        String.format(TranslationUtils.getTranslation().skinActionAffectedProfile,
                                String.join(", ", profiles.stream().map(GameProfile::getName).toList()))), true);
                
                if (!players.isEmpty()) {
                    src.sendFeedback(() -> Text.of(
                            String.format(TranslationUtils.getTranslation().skinActionAffectedPlayer,
                                    String.join(", ", players.stream().map(p -> p.getGameProfile().getName()).toList()))), true);
                }
            } else {
                src.sendFeedback(() -> Text.of(TranslationUtils.getTranslation().skinActionOk), true);
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
