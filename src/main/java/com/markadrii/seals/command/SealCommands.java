package com.markadrii.seals.command;

import com.markadrii.seals.Seals;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.SealRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * Comandos de prueba y administración (nivel de permiso 2: operadores o trucos activados).
 * <pre>
 * /seals info [jugadores]
 * /seals seal &lt;mob&gt; [jugadores] [edad]
 * /seals unseal [jugadores]
 * /seals age set|add &lt;años&gt; [jugadores]
 * /seals affinity set|add &lt;cantidad&gt; [jugadores]
 * /seals stars set|add &lt;estrellas&gt; [jugadores]
 * /seals stability set &lt;valor&gt; [jugadores]
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Seals.MOD_ID)
public final class SealCommands {

    private static final DynamicCommandExceptionType UNKNOWN_SEAL = new DynamicCommandExceptionType(id ->
            Component.translatable("command.seals.unknown_seal", id));

    private static final SuggestionProvider<CommandSourceStack> SEAL_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(SealRegistry.all().stream().map(SealDefinition::id), builder);

    private SealCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private interface PlayerAction {
        /** @return true si se ha aplicado al jugador. */
        boolean run(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seals")
                .requires(src -> src.hasPermission(2))

                // /seals info
                .then(withTargets(Commands.literal("info"), SealCommands::info))

                // /seals seal <mob> [jugadores] [edad]
                .then(Commands.literal("seal")
                        .then(Commands.argument("mob", ResourceLocationArgument.id()).suggests(SEAL_SUGGESTIONS)
                                .executes(ctx -> sealSelf(ctx, -1))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> sealTargets(ctx, -1))
                                        .then(Commands.argument("age", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> sealTargets(ctx, IntegerArgumentType.getInteger(ctx, "age")))))))

                // /seals unseal [jugadores]
                .then(withTargets(Commands.literal("unseal"), SealCommands::unseal))

                // /seals age set|add <años>
                .then(Commands.literal("age")
                        .then(withAmount("set", 0, 10000, SealCommands::ageSet))
                        .then(withAmount("add", -10000, 10000, SealCommands::ageAdd)))

                // /seals affinity set|add <cantidad>
                .then(Commands.literal("affinity")
                        .then(withAmount("set", 0, 100, SealCommands::affinitySet))
                        .then(withAmount("add", -10000, 10000, SealCommands::affinityAdd)))

                // /seals stars set|add <estrellas>
                .then(Commands.literal("stars")
                        .then(withAmount("set", 0, 100, SealCommands::starsSet))
                        .then(withAmount("add", -100, 100, SealCommands::starsAdd)))

                // /seals stability set <valor>
                .then(Commands.literal("stability")
                        .then(withAmount("set", 0, 100, SealCommands::stabilitySet)))
        );
    }

    // ---------------------------------------------------------------------------------------
    // Constructores de argumentos
    // ---------------------------------------------------------------------------------------

    private static ArgumentBuilder<CommandSourceStack, ?> withTargets(
            ArgumentBuilder<CommandSourceStack, ?> node, PlayerAction action) {
        return node
                .executes(ctx -> apply(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), action))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> apply(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), action)));
    }

    private interface AmountAction {
        boolean run(CommandSourceStack source, ServerPlayer player, int amount) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> withAmount(String literal, int min, int max, AmountAction action) {
        return Commands.literal(literal)
                .then(Commands.argument("amount", IntegerArgumentType.integer(min, max))
                        .executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            return apply(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()),
                                    (src, p) -> action.run(src, p, amount));
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    return apply(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                            (src, p) -> action.run(src, p, amount));
                                })));
    }

    private static int apply(CommandSourceStack source, Collection<ServerPlayer> players, PlayerAction action)
            throws CommandSyntaxException {
        int done = 0;
        for (ServerPlayer player : players) {
            if (action.run(source, player)) done++;
        }
        return done;
    }

    /** Devuelve los datos si el jugador tiene un sello; si no, avisa y devuelve null. */
    private static SealData requireSeal(CommandSourceStack source, ServerPlayer player) {
        SealData data = SealCapability.get(player);
        if (!data.hasSeal() || data.definition() == null) {
            source.sendFailure(Component.translatable("command.seals.no_seal", player.getDisplayName()));
            return null;
        }
        return data;
    }

    private static void ok(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message.copy().withStyle(ChatFormatting.GREEN), true);
    }

    // ---------------------------------------------------------------------------------------
    // Comandos
    // ---------------------------------------------------------------------------------------

    private static boolean info(CommandSourceStack source, ServerPlayer player) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        SealDefinition def = data.definition();
        source.sendSuccess(() -> Component.translatable("command.seals.info", player.getDisplayName(), def.name(),
                data.level() - 1, def.maxStars(), data.affinity(), data.stability(),
                data.ageYears(), data.ageMonths() % 12).withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    private static int sealSelf(CommandContext<CommandSourceStack> ctx, int age) throws CommandSyntaxException {
        return seal(ctx, List.of(ctx.getSource().getPlayerOrException()), age);
    }

    private static int sealTargets(CommandContext<CommandSourceStack> ctx, int age) throws CommandSyntaxException {
        return seal(ctx, EntityArgument.getPlayers(ctx, "targets"), age);
    }

    private static int seal(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, int age)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "mob");
        SealDefinition def = SealRegistry.get(id);
        if (def == null) throw UNKNOWN_SEAL.create(id.toString());

        return apply(ctx.getSource(), players, (src, player) -> {
            SealData data = SealCapability.get(player);
            if (data.definition() != null) data.definition().deactivateAll(player, data);
            int years = age >= 0 ? age : def.minAge() + player.getRandom().nextInt(def.maxAge() - def.minAge() + 1);
            data.seal(def.id(), years, SealManager.currentDay(player.server));
            SealManager.sync(player);
            player.sendSystemMessage(Component.translatable("message.seals.sealed", def.name(), years)
                    .withStyle(ChatFormatting.GOLD));
            ok(src, Component.translatable("command.seals.sealed", player.getDisplayName(), def.name(), years));
            return true;
        });
    }

    private static boolean unseal(CommandSourceStack source, ServerPlayer player) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        Component name = data.definition().name();
        data.definition().deactivateAll(player, data);
        data.clearSeal();
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.unsealed", player.getDisplayName(), name));
        return true;
    }

    private static boolean ageSet(CommandSourceStack source, ServerPlayer player, int years) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        data.setAgeMonths(years * 12);
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.age", player.getDisplayName(), data.ageYears()));
        return true;
    }

    /** Sumar años cuenta como cumpleaños: cada año da +70 de afinidad. */
    private static boolean ageAdd(CommandSourceStack source, ServerPlayer player, int years) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        if (years > 0) {
            for (int i = 0; i < years; i++) {
                data.setAgeMonths(data.ageMonths() + 12);
                SealManager.addAffinity(player, data, 70, "affinity.seals.reason.birthday");
            }
        } else {
            data.setAgeMonths(Math.max(0, data.ageMonths() + years * 12));
        }
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.age", player.getDisplayName(), data.ageYears()));
        return true;
    }

    private static boolean affinitySet(CommandSourceStack source, ServerPlayer player, int amount) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        data.setAffinity(amount);
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.affinity", player.getDisplayName(), data.affinity(), data.level() - 1));
        return true;
    }

    /** Sumar afinidad sube estrellas igual que en el juego normal. */
    private static boolean affinityAdd(CommandSourceStack source, ServerPlayer player, int amount) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        if (amount >= 0) {
            SealManager.addAffinity(player, data, amount, null);
        } else {
            data.setAffinity(Math.max(0, data.affinity() + amount));
            SealManager.sync(player);
        }
        ok(source, Component.translatable("command.seals.affinity", player.getDisplayName(), data.affinity(), data.level() - 1));
        return true;
    }

    private static boolean starsSet(CommandSourceStack source, ServerPlayer player, int stars) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        return setLevel(source, player, data, stars + 1);
    }

    private static boolean starsAdd(CommandSourceStack source, ServerPlayer player, int stars) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        return setLevel(source, player, data, data.level() + stars);
    }

    private static boolean setLevel(CommandSourceStack source, ServerPlayer player, SealData data, int level) {
        SealDefinition def = data.definition();
        int newLevel = Math.max(1, Math.min(def.maxLevel(), level));
        if (newLevel < data.level()) {
            def.deactivateAll(player, data); // apaga habilidades que puedan quedar bloqueadas
        }
        data.setLevel(newLevel);
        data.setAffinity(0);
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.stars", player.getDisplayName(), newLevel - 1, def.maxStars()));
        return true;
    }

    private static boolean stabilitySet(CommandSourceStack source, ServerPlayer player, int value) {
        SealData data = requireSeal(source, player);
        if (data == null) return false;
        if (value <= 0) {
            SealManager.breakSeal(player, data);
            ok(source, Component.translatable("command.seals.broken", player.getDisplayName()));
            return true;
        }
        data.setStability(value);
        SealManager.sync(player);
        ok(source, Component.translatable("command.seals.stability", player.getDisplayName(), data.stability()));
        return true;
    }
}
