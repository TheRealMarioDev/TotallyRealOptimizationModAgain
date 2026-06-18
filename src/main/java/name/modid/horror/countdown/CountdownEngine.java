package name.modid.horror.countdown;

import name.modid.horror.HorrorOperator;
import name.modid.horror.network.CountdownPayload;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.network.PanicScarePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distance countdown that always decreases by 1 block,
 * but accelerates by reducing the time between updates.
 */
public final class CountdownEngine {

    public static final int START_DISTANCE = 100;

    public static final int BASE_DECREMENT = 1;

    public static final int RUN_THRESHOLD = 35;

    /** Starts at 1 second between decreases. */
    private static final int START_INTERVAL = 20;

    /** Fastest speed possible (every tick). */
    private static final int MIN_INTERVAL = 1;

    /** Lower = faster acceleration. */
    private static final double SPEEDUP = 0.97;

    private static final Map<UUID, CountdownTask> ACTIVE_COUNTDOWNS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    public static class CountdownTask {

        int blocksLeft;

        final net.minecraft.resources.ResourceLocation tickSound;
        final net.minecraft.resources.ResourceLocation endSound;

        int ticksElapsed = 0;
        int stepsElapsed = 0;

        boolean creepStarted = false;

        double currentInterval = START_INTERVAL;
        double tickAccumulator = 0;

        CountdownTask(
                int blocksLeft,
                int baseDecrement,
                String tickSoundStr,
                String endSoundStr
        ) {
            this.blocksLeft = blocksLeft;

            this.tickSound =
                    (tickSoundStr != null && !tickSoundStr.isEmpty())
                            ? new net.minecraft.resources.ResourceLocation(tickSoundStr)
                            : null;

            this.endSound =
                    (endSoundStr != null && !endSoundStr.isEmpty())
                            ? new net.minecraft.resources.ResourceLocation(endSoundStr)
                            : null;
        }

        int tickSoundInterval() {
            return Math.max(1, 5 - stepsElapsed / 5);
        }

        float tickSoundPitch() {
            return Math.min(2.0f, 0.75f + stepsElapsed * 0.035f);
        }

        float tickSoundVolume() {
            return Math.min(1.0f, 0.55f + stepsElapsed * 0.025f);
        }
    }

    private CountdownEngine() {}

    public static void register() {
        if (registered) return;

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(CountdownEngine::tick);

        HorrorOperator.LOGGER.info("[HorrorOperator] CountdownEngine registered.");
    }

    public static void start(
            ServerPlayer target,
            int startDistance,
            int baseDecrement,
            String tickSound,
            String endSound
    ) {
        ACTIVE_COUNTDOWNS.put(
                target.getUUID(),
                new CountdownTask(
                        startDistance,
                        baseDecrement,
                        tickSound,
                        endSound
                )
        );

        HorrorOperator.LOGGER.info(
                "[HorrorOperator] Countdown started for {} (start={}).",
                target.getGameProfile().getName(),
                startDistance
        );
    }

    public static void cancel(ServerPlayer target) {
        if (ACTIVE_COUNTDOWNS.remove(target.getUUID()) != null) {
            HorrorOperator.LOGGER.info(
                    "[HorrorOperator] Countdown cancelled for {}.",
                    target.getGameProfile().getName()
            );
        }
    }

    public static boolean isActive(ServerPlayer target) {
        return ACTIVE_COUNTDOWNS.containsKey(target.getUUID());
    }

    private static void tick(MinecraftServer server) {

        if (ACTIVE_COUNTDOWNS.isEmpty()) {
            return;
        }

        ACTIVE_COUNTDOWNS.entrySet().removeIf(entry -> {

            UUID targetId = entry.getKey();
            CountdownTask task = entry.getValue();

            task.ticksElapsed++;

            ServerPlayer target =
                    server.getPlayerList().getPlayer(targetId);

            if (target == null) {
                return true;
            }

            task.tickAccumulator++;

            if (task.tickAccumulator < task.currentInterval) {
                return false;
            }

            task.tickAccumulator = 0;

            // Always decrease by exactly 1
            task.blocksLeft--;

            task.stepsElapsed++;

            // Accelerate countdown
            task.currentInterval =
                    Math.max(
                            MIN_INTERVAL,
                            task.currentInterval * SPEEDUP
                    );

            if (task.blocksLeft <= 0) {

                if (task.endSound != null) {
                    target.playNotifySound(
                            SoundEvent.createVariableRangeEvent(task.endSound),
                            SoundSource.MASTER,
                            1.0f,
                            0.9f
                    );
                }

                HorrorNetworking.sendTo(
                        target,
                        new CountdownPayload(0, true)
                );

                HorrorNetworking.sendTo(
                        target,
                        new PanicScarePayload(
                                PanicScarePayload.TOTAL_DISTORT
                        )
                );

                HorrorOperator.LOGGER.info(
                        "[HorrorOperator] Countdown finished for {} — panic fired.",
                        target.getGameProfile().getName()
                );

                return true;
            }

            if (!task.creepStarted &&
                    task.blocksLeft <= RUN_THRESHOLD) {

                task.creepStarted = true;

                HorrorNetworking.sendTo(
                        target,
                        new PanicScarePayload(
                                PanicScarePayload.CREEP_DISTORT
                        )
                );
            }

            if (task.ticksElapsed % task.tickSoundInterval() == 0
                    && task.tickSound != null) {

                target.playNotifySound(
                        SoundEvent.createVariableRangeEvent(task.tickSound),
                        SoundSource.MASTER,
                        task.tickSoundVolume(),
                        task.tickSoundPitch()
                );
            }

            HorrorNetworking.sendTo(
                    target,
                    new CountdownPayload(
                            task.blocksLeft,
                            false
                    )
            );

            return false;
        });
    }
}