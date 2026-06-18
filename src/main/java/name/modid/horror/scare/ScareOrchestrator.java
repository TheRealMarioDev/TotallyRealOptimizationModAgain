package name.modid.horror.scare;

import name.modid.horror.config.HorrorConfigManager;
import name.modid.horror.countdown.CountdownEngine;
import name.modid.horror.curse.CurseManager;
import name.modid.horror.fromthefog.FogIntegration;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.network.PanicScarePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Combines multiple scare subsystems into one-shot sequences for operators.
 */
public final class ScareOrchestrator {

    private ScareOrchestrator() {}

    /** Instant post-processing panic shader only. */
    public static void panic(ServerPlayer target) {
        HorrorNetworking.sendTo(
                target,
                new PanicScarePayload(PanicScarePayload.TOTAL_DISTORT)
        );
    }

    /** Clears shader, overlay, countdown, and optional FTF presence. */
    public static void reset(ServerPlayer target, MinecraftServer server) {

        CountdownEngine.cancel(target);

        HorrorNetworking.sendTo(
                target,
                new PanicScarePayload(PanicScarePayload.RESET)
        );

        if (FogIntegration.isFtfLoaded()) {
            FogIntegration.clearHerobrinePresence(target, server);
        }
    }

    /**
     * Full scare:
     * Countdown → RUN phase → jumpscare audio → panic shader.
     */
    public static void fullSequence(
            ServerPlayer target,
            MinecraftServer server
    ) {

        startCountdown(
                target,
                server,
                HorrorConfigManager.getConfig()
                        .countdown
                        .startDistance,

                CountdownEngine.BASE_DECREMENT
        );
    }

    /**
     * Trap scare:
     * Curse + FTF presence + countdown.
     */
    public static void trapSequence(
            ServerPlayer target,
            MinecraftServer server
    ) {

        CurseManager.flagWithCurse(target);

        if (FogIntegration.isFtfLoaded()) {

            FogIntegration.applyHerobrinePresence(
                    target,
                    server,

                    HorrorConfigManager.getConfig()
                            .countdown
                            .startDistance
            );
        }

        startCountdown(
                target,
                server,

                HorrorConfigManager.getConfig()
                        .countdown
                        .startDistance,

                CountdownEngine.BASE_DECREMENT
        );
    }

    /**
     * Panic shader layered on top of an already-running countdown.
     */
    public static void panicDuringCountdown(ServerPlayer target) {

        HorrorNetworking.sendTo(
                target,
                new PanicScarePayload(PanicScarePayload.TOTAL_DISTORT)
        );
    }

    public static void startCountdown(
            ServerPlayer target,
            MinecraftServer server,
            int startDistance,
            int baseDecrement
    ) {

        startCountdown(
                target,
                server,
                startDistance,
                baseDecrement,

                HorrorConfigManager.getConfig()
                        .sounds
                        .tickSound,

                HorrorConfigManager.getConfig()
                        .sounds
                        .endSound
        );
    }

    public static void startCountdown(
            ServerPlayer target,
            MinecraftServer server,
            int startDistance,
            int baseDecrement,
            String tickSound,
            String endSound
    ) {

        CountdownEngine.start(
                target,
                startDistance,
                baseDecrement,
                tickSound,
                endSound
        );

        if (FogIntegration.isFtfLoaded()) {

            FogIntegration.applyHerobrinePresence(
                    target,
                    server,
                    startDistance
            );
        }
    }
}