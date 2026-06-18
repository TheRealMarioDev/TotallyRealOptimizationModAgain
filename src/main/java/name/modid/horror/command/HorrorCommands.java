package name.modid.horror.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import name.modid.horror.HorrorOperator;
import name.modid.horror.countdown.CountdownEngine;
import name.modid.horror.curse.CurseManager;
import name.modid.horror.fromthefog.FogIntegration;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.network.PanicScarePayload;
import name.modid.horror.scare.AmbientScareManager;
import name.modid.horror.scare.ScareOrchestrator;
import name.modid.horror.spy.SpyNetworking;
import name.modid.horror.vanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import name.modid.horror.config.HorrorConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey; // Add this import

/**
 * HorrorOperator slash-commands (OP level 2).
 *
 * <h2>Scare commands</h2>
 * <pre>
 * /scare full {@literal <player>}     — countdown → RUN → jumpscare → panic shader
 * /scare trap {@literal <player>}     — curse + FTF presence + full sequence
 * /scare panic {@literal <player>} [intensity] — instant shader (default 2)
 * /scare countdown {@literal <player>} [start] [baseDec] [tickSound] [endSound]
 * /scare whisper {@literal <player>} [message] — sends a private Herobrine chat line
 * /scare stalk {@literal <player>} [seconds] — timed footsteps, whispers, FTF pressure, panic finish
 * /scare omen {@literal <player>} — thunder flash + hostile sounds + close FTF presence
 * /scare mark {@literal <player>} — builds a tiny shrine behind the target if there is safe air
 * /scare echo {@literal <player>} [message] — sends a scary echo message
 * /scare shadow {@literal <player>} — casts a dark shadow that glows
 * /scare nightmare {@literal <player>} — inflicts nausea and slowness
 * /scare reset {@literal <player>}     — clears everything
 * </pre>
 */
public final class HorrorCommands {

    private HorrorCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            net.minecraft.commands.CommandBuildContext registryAccess,
            net.minecraft.commands.Commands.CommandSelection environment
    ) {
        dispatcher.register(
                Commands.literal("vanish")
                        .requires(src -> src.hasPermission(2))
                        .executes(HorrorCommands::executeVanish)
        );

        var playerArg = Commands.argument("player", EntityArgument.player());

        dispatcher.register(
                Commands.literal("scare")
                        .requires(src -> src.hasPermission(2))

                        // /scare full <player>
                        .then(Commands.literal("full")
                                .then(playerArg.executes(HorrorCommands::executeScareFull))
                        )

                        // /scare trap <player>
                        .then(Commands.literal("trap")
                                .then(playerArg.executes(HorrorCommands::executeScareTrap))
                        )

                        // /scare panic <player> [intensity]
                        .then(Commands.literal("panic")
                                .then(playerArg
                                        .executes(ctx -> executeScarePanic(ctx, PanicScarePayload.TOTAL_DISTORT))
                                        .then(Commands.argument("intensity", IntegerArgumentType.integer(0, 3))
                                                .executes(HorrorCommands::executeScarePanic)
                                        )
                                )
                        )

                        // /scare countdown <player> [startDistance] [decrement] [tickSound] [endSound]
                        .then(Commands.literal("countdown")
                                .then(playerArg
                                        .executes(HorrorCommands::executeScareCountdown)
                                        .then(Commands.argument("startDistance", IntegerArgumentType.integer(1))
                                                .executes(HorrorCommands::executeScareCountdown)
                                                .then(Commands.argument("decrement", IntegerArgumentType.integer(1))
                                                        .executes(HorrorCommands::executeScareCountdown)
                                                        .then(Commands.argument("tickSound", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                                .executes(HorrorCommands::executeScareCountdown)
                                                                .then(Commands.argument("endSound", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                                        .executes(HorrorCommands::executeScareCountdown)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )

                        // /scare whisper <player> [message]
                        .then(Commands.literal("whisper")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> executeScareWhisper(ctx, "I am already here"))
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(HorrorCommands::executeScareWhisper)
                                        )
                                )
                        )

                        // /scare stalk <player> [seconds]
                        .then(Commands.literal("stalk")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> executeScareStalk(ctx, 45))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 180))
                                                .executes(HorrorCommands::executeScareStalk)
                                        )
                                )
                        )

                        // /scare omen <player>
                        .then(Commands.literal("omen")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(HorrorCommands::executeScareOmen)
                                )
                        )

                        // /scare mark <player>
                        .then(Commands.literal("mark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(HorrorCommands::executeScareMark)
                                )
                        )

                        // /scare echo <player> [message]
                        .then(Commands.literal("echo")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> executeScareEcho(ctx, "Your screams echo in an empty room."))
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(HorrorCommands::executeScareEcho)
                                        )
                                )
                        )

                        // /scare shadow <player>
                        .then(Commands.literal("shadow")
                                .then(playerArg.executes(HorrorCommands::executeScareShadow))
                        )

                        // /scare nightmare <player>
                        .then(Commands.literal("nightmare")
                                .then(playerArg.executes(HorrorCommands::executeScareNightmare))
                        )

                        // /scare reset <player>
                        .then(Commands.literal("reset")
                                .then(playerArg.executes(HorrorCommands::executeScareReset))
                        )
        );

        dispatcher.register(
                Commands.literal("curse")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("herobrine")
                                .then(playerArg.executes(HorrorCommands::executeCurseHerobrine))
                        )
                        .then(Commands.literal("clear")
                                .then(playerArg.executes(HorrorCommands::executeCurseClear))
                        )
        );

        dispatcher.register(
                Commands.literal("spy")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("inventory")
                                .then(playerArg.executes(HorrorCommands::executeSpyInventory))
                        )
        );

        HorrorOperator.LOGGER.info("[HorrorOperator] Commands registered.");
    }

    private static int executeVanish(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer admin;
        try {
            admin = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        boolean nowVanished = VanishManager.toggle(admin);
        if (nowVanished) {
            admin.sendSystemMessage(Component.literal("§8[§7Admin§8] §7You are now §0invisible§7."));
            Component leaveMsg = Component.translatable("multiplayer.player.left", admin.getDisplayName())
                    .withStyle(net.minecraft.ChatFormatting.YELLOW);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(leaveMsg, false);
        } else {
            admin.sendSystemMessage(Component.literal("§8[§7Admin§8] §7You are now §fvisible§7."));
            Component joinMsg = Component.translatable("multiplayer.player.joined", admin.getDisplayName())
                    .withStyle(net.minecraft.ChatFormatting.YELLOW);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(joinMsg, false);
        }
        return 1;
    }

    /** /scare full <player> */
    private static int executeScareFull(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            ScareOrchestrator.fullSequence(target, ctx.getSource().getServer());
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Full scare sequence started for " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare trap <player> */
    private static int executeScareTrap(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            ScareOrchestrator.trapSequence(target, ctx.getSource().getServer());
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Trap sequence started for " + target.getGameProfile().getName()
                            + " (curse + countdown + panic)"),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare panic <player> [intensity] */
    private static int executeScarePanic(CommandContext<CommandSourceStack> ctx) {
        int intensity;
        try {
            intensity = IntegerArgumentType.getInteger(ctx, "intensity");
        } catch (IllegalArgumentException e) {
            intensity = PanicScarePayload.TOTAL_DISTORT;
        }
        return executeScarePanic(ctx, intensity);
    }

    private static int executeScarePanic(CommandContext<CommandSourceStack> ctx, int intensity) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            HorrorNetworking.sendTo(target, new PanicScarePayload(intensity));
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Panic scare sent to " + target.getGameProfile().getName()
                            + " (intensity=" + intensity + ")"),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare countdown <player> [startDistance] [decrement] [tickSound] [endSound] */
    private static int executeScareCountdown(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            int startDistance = CountdownEngine.START_DISTANCE;
            int decrement = CountdownEngine.BASE_DECREMENT;

            String tickSound =
                    HorrorConfigManager.getConfig()
                            .sounds
                            .tickSound;

            String endSound =
                    HorrorConfigManager.getConfig()
                            .sounds
                            .endSound;
            try { startDistance = IntegerArgumentType.getInteger(ctx, "startDistance"); } catch (IllegalArgumentException ignored) {}
            try { decrement = IntegerArgumentType.getInteger(ctx, "decrement"); } catch (IllegalArgumentException ignored) {}
            try { tickSound = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "tickSound").toString(); } catch (IllegalArgumentException ignored) {}
            try { endSound = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "endSound").toString(); } catch (IllegalArgumentException ignored) {}

            ScareOrchestrator.startCountdown(target, ctx.getSource().getServer(), startDistance, decrement, tickSound, endSound);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Countdown started for " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare reset <player> */
    private static int executeScareReset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            ScareOrchestrator.reset(target, ctx.getSource().getServer());
            AmbientScareManager.reset(target, ctx.getSource().getServer());
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Scare reset for " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare whisper <player> [message] */
    private static int executeScareWhisper(CommandContext<CommandSourceStack> ctx) {
        try {
            String message;
            try {
                message = StringArgumentType.getString(ctx, "message");
            } catch (IllegalArgumentException e) {
                message = "I am already here";
            }
            return executeScareWhisper(ctx, message);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeScareWhisper(CommandContext<CommandSourceStack> ctx, String message) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            AmbientScareManager.whisper(target, message);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Herobrine whisper sent to " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare stalk <player> [seconds] */
    private static int executeScareStalk(CommandContext<CommandSourceStack> ctx) {
        int seconds;
        try {
            seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        } catch (IllegalArgumentException e) {
            seconds = 45;
        }
        return executeScareStalk(ctx, seconds);
    }

    private static int executeScareStalk(CommandContext<CommandSourceStack> ctx, int seconds) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            AmbientScareManager.stalk(target, ctx.getSource().getServer(), seconds);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Stalk sequence started for " + target.getGameProfile().getName()
                            + " (" + seconds + "s)"),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare omen <player> */
    private static int executeScareOmen(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            AmbientScareManager.omen(target, ctx.getSource().getServer());
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Omen fired for " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare mark <player> */
    private static int executeScareMark(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            boolean placed = AmbientScareManager.mark(target);
            if (!placed) {
                ctx.getSource().sendFailure(Component.literal("Could not find safe air behind " + target.getGameProfile().getName()));
                return 0;
            }
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Mark placed behind " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeCurseHerobrine(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            CurseManager.flagWithCurse(target);
            if (FogIntegration.isFtfLoaded()) {
                FogIntegration.applyHerobrinePresence(target, ctx.getSource().getServer(), 5);
            }
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Herobrine Curse applied to " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeCurseClear(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            CurseManager.clearCurse(target);
            if (FogIntegration.isFtfLoaded()) {
                FogIntegration.clearHerobrinePresence(target, ctx.getSource().getServer());
            }
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Herobrine Curse cleared from " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSpyInventory(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer admin = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

            if (target.getUUID().equals(admin.getUUID())) {
                ctx.getSource().sendFailure(Component.literal("You cannot spy on yourself."));
                return 0;
            }

            SpyNetworking.openSpyFor(admin, target);
            admin.sendSystemMessage(Component.literal("§8[Admin] §7Opening spy view for §f" + target.getGameProfile().getName() + "§7..."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare echo <player> [message] */
    private static int executeScareEcho(CommandContext<CommandSourceStack> ctx) {
        try {
            String message;
            try {
                message = StringArgumentType.getString(ctx, "message");
            } catch (IllegalArgumentException e) {
                message = "Your screams echo in an empty room.";
            }
            return executeScareEcho(ctx, message);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeScareEcho(CommandContext<CommandSourceStack> ctx, String message) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            target.sendSystemMessage(Component.literal("§8<§fHerobrine§8> §7" + message));
            play(target, "minecraft:entity.enderman.ambient", SoundSource.HOSTILE, 0.55f, 0.55f);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Echo sent to " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare shadow <player> */
    private static int executeScareShadow(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            target.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    200,
                    1,
                    false,
                    false,
                    true
            ));
            target.displayClientMessage(Component.literal("§8A dark shadow surrounds you..."), true);
            play(target, "minecraft:entity.warden.ambient", SoundSource.HOSTILE, 0.8f, 0.5f);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Shadow cast upon " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    /** /scare nightmare <player> */
    private static int executeScareNightmare(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            target.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION,
                    200,
                    1,
                    false,
                    false,
                    true
            ));
            target.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN,
                    200,
                    1,
                    false,
                    false,
                    true
            ));
            target.displayClientMessage(Component.literal("§8§kYou feel trapped in a nightmare..."), true);
            play(target, "minecraft:ambient.cave", SoundSource.AMBIENT, 0.9f, 0.5f);
            play(target, "minecraft:entity.wither.ambient", SoundSource.HOSTILE, 0.7f, 0.6f);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§8[Horror] Nightmare inflicted upon " + target.getGameProfile().getName()),
                    false
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static void play(ServerPlayer target, String soundId, SoundSource source, float volume, float pitch) {
        target.playNotifySound(
                SoundEvent.createVariableRangeEvent(new ResourceLocation(soundId)),
                source,
                volume,
                pitch
        );
    }
}
