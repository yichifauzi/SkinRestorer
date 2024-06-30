package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.TranslationUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SkinCommand {
    
    private SkinCommand() {}
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> base =
                literal("skin")
                        .then(buildAction("clear", () -> Result.ofNullable(null)));
        
        LiteralArgumentBuilder<CommandSourceStack> set = literal("set");
        
        for (Map.Entry<String, SkinProvider> entry : SkinRestorer.getProviders()) {
            set.then(buildAction(entry.getKey(), entry.getValue()));
        }
        
        base.then(set);
        
        dispatcher.register(base);
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> buildAction(String name, SkinProvider provider) {
        LiteralArgumentBuilder<CommandSourceStack> action = literal(name);
        
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
    
    private static ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> buildAction(
            String name,
            Supplier<Result<Optional<Property>, Exception>> supplier
    ) {
        return buildArgument(literal(name), context -> supplier.get());
    }
    
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> buildArgument(
            ArgumentBuilder<CommandSourceStack, T> argument,
            Function<CommandContext<CommandSourceStack>, Result<Optional<Property>, Exception>> provider
    ) {
        return argument
                .executes(context -> skinAction(
                        context.getSource(),
                        () -> provider.apply(context)
                ))
                .then(makeTargetsArgument(provider));
    }
    
    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> makeTargetsArgument(
            Function<CommandContext<CommandSourceStack>, Result<Optional<Property>, Exception>> provider
    ) {
        return argument("targets", GameProfileArgument.gameProfile())
                .requires(source -> source.hasPermission(2))
                .executes(context -> skinAction(
                        context.getSource(),
                        GameProfileArgument.getGameProfiles(context, "targets"),
                        true,
                        () -> provider.apply(context)
                ));
    }
    
    private static int skinAction(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            boolean setByOperator,
            Supplier<Result<Optional<Property>, Exception>> skinSupplier
    ) {
        SkinRestorer.setSkinAsync(src.getServer(), targets, skinSupplier).thenAccept(result -> {
            if (result.isError()) {
                src.sendFailure(TranslationUtils.translatableWithFallback(
                        TranslationUtils.COMMAND_SKIN_FAILED_KEY,
                        result.getErrorValue()
                ));
                return;
            }
            
            var updatedPlayers = result.getSuccessValue();
            
            if (setByOperator) {
                if (!updatedPlayers.isEmpty()) {
                    var playersComponent = Component.empty();
                    int index = 0;
                    for (var player : updatedPlayers) {
                        playersComponent.append(Objects.requireNonNull(player.getDisplayName()));
                        index++;
                        if (index < updatedPlayers.size())
                            playersComponent.append(", ");
                    }
                    
                    src.sendSuccess(() -> TranslationUtils.translatableWithFallback(
                            TranslationUtils.COMMAND_SKIN_AFFECTED_PLAYERS_KEY,
                            playersComponent
                    ), true);
                }
            } else {
                src.sendSuccess(() -> TranslationUtils.translatableWithFallback(
                        TranslationUtils.COMMAND_SKIN_OK_KEY
                ), true);
            }
        });
        
        return targets.size();
    }
    
    private static int skinAction(CommandSourceStack src, Supplier<Result<Optional<Property>, Exception>> skinSupplier) {
        if (src.getPlayer() == null)
            return 0;
        
        return skinAction(src, Collections.singleton(src.getPlayer().getGameProfile()), false, skinSupplier);
    }
}
