package ru.exeswi.exest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.events.HorrorEvent;
import ru.exeswi.exest.events.HorrorEventManager;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.world.HorrorWorldState;

/**
 * /horror — /horror status is available to every player; the admin tools (reload,
 * sanity set, forcing events, debug mode) each require op level 2.
 */
public final class HorrorCommand {

    private HorrorCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("horror")
                .then(CommandManager.literal("status").executes(HorrorCommand::status))
                .then(CommandManager.literal("stats").executes(HorrorCommand::stats))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ConfigManager.load();
                            ctx.getSource().sendFeedback(() -> Text.literal("Horror Mode config reloaded"), true);
                            return 1;
                        }))
                .then(CommandManager.literal("difficulty")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            int level = DifficultyScaler.level(ctx.getSource().getWorld());
                            long ticks = HorrorWorldState.get(ctx.getSource().getWorld()).horrorTicks;
                            ctx.getSource().sendFeedback(() -> Text.literal(
                                    "Horror level: " + level + "/10 (" + ticks / 24000 + " haunted days)"), false);
                            return level;
                        }))
                .then(CommandManager.literal("debug")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
                        .then(CommandManager.literal("off").executes(ctx -> setDebug(ctx.getSource(), false))))
                .then(CommandManager.literal("sanity")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            float value = SanityManager.get(player);
                            ctx.getSource().sendFeedback(() -> Text.literal(
                                    String.format("Sanity: %.1f/100", value)), false);
                            return (int) value;
                        })
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            HorrorWorldState.get(player.getServerWorld())
                                                    .setSanity(player.getUuid(), value);
                                            SanityManager.forceSync(player);
                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                    "Sanity set to " + value), true);
                                            return 1;
                                        }))))
                .then(CommandManager.literal("event")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                        CommandSource.suggestMatching(HorrorEventManager.eventIds(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    HorrorEvent event = HorrorEventManager.byId(id);
                                    if (event == null) {
                                        ctx.getSource().sendError(Text.literal("Unknown event: " + id));
                                        return 0;
                                    }
                                    Exest.eventManager().runNow(event, player);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Fired event " + id), true);
                                    return 1;
                                }))));
    }

    private static int status(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        int level = DifficultyScaler.level(player.getServerWorld());
        float sanity = SanityManager.get(player);
        long day = player.getServerWorld().getTime() / 24000 + 1;
        int nearby = player.getServerWorld().getEntitiesByClass(AbstractHorrorEntity.class,
                Box.of(player.getPos(), 96, 48, 96), e -> !e.isApparition()).size();

        Formatting sanityColor = sanity > 66 ? Formatting.GREEN
                : sanity > 33 ? Formatting.YELLOW : Formatting.RED;
        ctx.getSource().sendFeedback(() -> Text.literal("— HORROR STATUS —").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Horror level: ")
                .append(Text.literal(level + "/10").formatted(Formatting.RED)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Mind: ")
                .append(Text.literal(String.format("%.0f/100", sanity)).formatted(sanityColor)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Day: " + day), false);
        ctx.getSource().sendFeedback(() -> nearby > 0
                ? Text.literal("Creatures nearby: " + nearby).formatted(Formatting.DARK_RED)
                : Text.literal("Creatures nearby: none... visible").formatted(Formatting.GRAY), false);
        return level;
    }

    private static int stats(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ctx.getSource().sendFeedback(() -> Text.literal("— ОНО ВЕЛО СЧЁТ —")
                .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        for (ru.exeswi.exest.stats.HorrorStats.Stat stat : ru.exeswi.exest.stats.HorrorStats.Stat.values()) {
            int value = ru.exeswi.exest.stats.HorrorStats.get(player, stat);
            Formatting color = value == 0 ? Formatting.DARK_GRAY : Formatting.GRAY;
            ctx.getSource().sendFeedback(() -> Text.literal(stat.label + ": ")
                    .formatted(color)
                    .append(Text.literal(String.valueOf(value))
                            .formatted(value == 0 ? Formatting.DARK_GRAY : Formatting.RED)), false);
        }
        return 1;
    }

    private static int setDebug(ServerCommandSource source, boolean value) {
        ConfigManager.get().debugMode = value;
        ConfigManager.save();
        source.sendFeedback(() -> Text.literal("Horror debug mode: " + (value ? "on" : "off")), true);
        return 1;
    }
}
