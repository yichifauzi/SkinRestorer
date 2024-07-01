package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Translation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SkinCommand {
    
    private SkinCommand() {}
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> base =
                literal("skin")
                        .then(buildAction("clear", () -> new SkinProviderContext("empty", null, null)));
        
        LiteralArgumentBuilder<CommandSourceStack> set = literal("set");
        
        var providers = SkinRestorer.getProvidersRegistry().getPublicProviders();
        for (var entry : providers)
            set.then(buildAction(entry.first(), entry.second()));
        
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
                                        context -> {
                                            var argument = StringArgumentType.getString(context, provider.getArgumentName());
                                            return new SkinProviderContext(name, argument, variant);
                                        }
                                ))
                );
            }
        } else {
            action.then(
                    buildArgument(
                            argument(provider.getArgumentName(), StringArgumentType.string()), context -> {
                                var argument = StringArgumentType.getString(context, provider.getArgumentName());
                                return new SkinProviderContext(name, argument, null);
                            }
                    )
            );
        }
        
        return action;
    }
    
    private static ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> buildAction(
            String name,
            Supplier<SkinProviderContext> supplier
    ) {
        return buildArgument(literal(name), context -> supplier.get());
    }
    
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> buildArgument(
            ArgumentBuilder<CommandSourceStack, T> argument,
            Function<CommandContext<CommandSourceStack>, SkinProviderContext> provider
    ) {
        return argument
                .executes(context -> skinAction(
                        context.getSource(),
                        provider.apply(context)
                ))
                .then(makeTargetsArgument(provider));
    }
    
    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> makeTargetsArgument(
            Function<CommandContext<CommandSourceStack>, SkinProviderContext> provider
    ) {
        return argument("targets", GameProfileArgument.gameProfile())
                .requires(source -> source.hasPermission(2))
                .executes(context -> skinAction(
                        provider.apply(context),
                        context.getSource(),
                        GameProfileArgument.getGameProfiles(context, "targets"),
                        true
                ));
    }
    
    private static int skinAction(
            SkinProviderContext context,
            CommandSourceStack src,
            Collection<GameProfile> targets,
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
        });
        
        return targets.size();
    }
    
    private static int skinAction(
            CommandSourceStack src,
            SkinProviderContext context
    ) {
        if (src.getPlayer() == null)
            return 0;
        
        return skinAction(context, src, Collections.singleton(src.getPlayer().getGameProfile()), false);
    }
    
    
}
