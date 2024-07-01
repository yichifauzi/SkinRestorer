package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Translation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SkinCommand {
    
    private SkinCommand() {}
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> base =
                literal("skin")
                        .then(buildSetAction("clear", SkinValue.EMPTY::toProviderContext))
                        .then(literal("reset")
                                .executes(context -> resetAction(context.getSource()))
                                .then(makeTargetsArgument(
                                        (context, profiles) -> resetAction(context.getSource(), profiles, true)
                                )));
        
        LiteralArgumentBuilder<CommandSourceStack> set = literal("set");
        
        var providers = SkinRestorer.getProvidersRegistry().getPublicProviders();
        for (var entry : providers)
            set.then(buildSetAction(entry.first(), entry.second()));
        
        base.then(set);
        
        dispatcher.register(base);
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> buildSetAction(String name, SkinProvider provider) {
        LiteralArgumentBuilder<CommandSourceStack> action = literal(name);
        
        if (provider.hasVariantSupport()) {
            for (SkinVariant variant : SkinVariant.values()) {
                action.then(
                        literal(variant.toString())
                                .then(buildSetArgument(
                                        argument(provider.getArgumentName(), StringArgumentType.string()),
                                        context -> {
                                            var argument = StringArgumentType.getString(context, provider.getArgumentName());
                                            return new SkinProviderContext(name, argument, variant);
                                        }
                                ))
                );
            }
        } else {
            action.then(
                    buildSetArgument(
                            argument(provider.getArgumentName(), StringArgumentType.string()),
                            context -> {
                                var argument = StringArgumentType.getString(context, provider.getArgumentName());
                                return new SkinProviderContext(name, argument, null);
                            }
                    )
            );
        }
        
        return action;
    }
    
    private static ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> buildSetAction(
            String name,
            Supplier<SkinProviderContext> supplier
    ) {
        return buildSetArgument(literal(name), context -> supplier.get());
    }
    
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> buildSetArgument(
            ArgumentBuilder<CommandSourceStack, T> argument,
            Function<CommandContext<CommandSourceStack>, SkinProviderContext> provider
    ) {
        return argument
                .executes(context -> setAction(
                        context.getSource(),
                        provider.apply(context)
                ))
                .then(makeTargetsArgument(
                        (context, targets) -> setAction(
                                context.getSource(),
                                targets,
                                provider.apply(context),
                                true
                        )
                ));
    }
    
    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> makeTargetsArgument(
            BiFunction<CommandContext<CommandSourceStack>, Collection<GameProfile>, Integer> consumer
    ) {
        return argument("targets", GameProfileArgument.gameProfile())
                .requires(source -> source.hasPermission(2))
                .executes(context -> consumer.apply(context, GameProfileArgument.getGameProfiles(context, "targets")));
    }
    
    private static int resetAction(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            boolean setByOperator
    ) {
        Collection<ServerPlayer> updatedPlayers = new HashSet<>();
        for (var profile : targets) {
            SkinValue skin = null;
            if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId()))
                skin = SkinRestorer.getSkinStorage().getSkin(profile.getId()).replaceValueWithOriginal();
            
            if (skin == null)
                continue;
            
            var updatedPlayer = SkinRestorer.applySkin(src.getServer(), Collections.singleton(profile), skin);
            
            SkinRestorer.getSkinStorage().deleteSkin(profile.getId());
            updatedPlayers.addAll(updatedPlayer);
        }
        
        SkinCommand.sendResponse(src, updatedPlayers, setByOperator);
        
        return targets.size();
    }
    
    private static int resetAction(
            CommandSourceStack src
    ) {
        if (src.getPlayer() == null)
            return 0;
        
        return resetAction(src, Collections.singleton(src.getPlayer().getGameProfile()), false);
    }
    
    private static int setAction(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            SkinProviderContext context,
            boolean setByOperator
    ) {
        src.sendSystemMessage(Translation.translatableWithFallback(Translation.COMMAND_SKIN_LOADING_KEY));
        
        SkinRestorer.setSkinAsync(src.getServer(), targets, context).thenAccept(result -> {
            if (result.isError()) {
                src.sendFailure(Translation.translatableWithFallback(
                        Translation.COMMAND_SKIN_FAILED_KEY,
                        result.getErrorValue()
                ));
                return;
            }
            
            var updatedPlayers = result.getSuccessValue();
            
            SkinCommand.sendResponse(src, updatedPlayers, setByOperator);
        });
        
        return targets.size();
    }
    
    private static int setAction(
            CommandSourceStack src,
            SkinProviderContext context
    ) {
        if (src.getPlayer() == null)
            return 0;
        
        return setAction(src, Collections.singleton(src.getPlayer().getGameProfile()), context, false);
    }
    
    private static void sendResponse(CommandSourceStack src, Collection<ServerPlayer> updatedPlayers, boolean setByOperator) {
        if (updatedPlayers.isEmpty()) {
            src.sendSuccess(() -> Translation.translatableWithFallback(
                    Translation.COMMAND_SKIN_NO_CHANGES_KEY
            ), true);
            return;
        }
        
        if (setByOperator) {
            var playersComponent = PlayerUtils.createPlayerListComponent(updatedPlayers);
            
            src.sendSuccess(() -> Translation.translatableWithFallback(
                    Translation.COMMAND_SKIN_AFFECTED_PLAYERS_KEY,
                    playersComponent
            ), true);
        } else {
            src.sendSuccess(() -> Translation.translatableWithFallback(
                    Translation.COMMAND_SKIN_OK_KEY
            ), true);
        }
    }
}
