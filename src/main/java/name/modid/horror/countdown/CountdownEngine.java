package name.modid.horror.countdown;

import name.modid.horror.HorrorOperator;
import name.modid.horror.config.HorrorConfigManager;
import name.modid.horror.network.CountdownPayload;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.network.PanicScarePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Distance countdown that always decreases by 1 block,
 * but accelerates by reducing the time between updates.
 */
public final class CountdownEngine {

    /**
     * Kept temporarily for compatibility.
     * We'll remove this when ScareOrchestrator is updated.
     */
    public static final int START_DISTANCE = 100;

    /**
     * Kept temporarily for compatibility.
     */
    public static final int BASE_DECREMENT = 1;

    private static final Map<UUID, CountdownTask> ACTIVE_COUNTDOWNS =
            new ConcurrentHashMap<>();

    private static boolean registered = false;
    private static final java.util.Random RANDOM = new java.util.Random();

    public static class CountdownTask {

        int blocksLeft;

        final net.minecraft.resources.ResourceLocation tickSound;
        final net.minecraft.resources.ResourceLocation endSound;

        int ticksElapsed = 0;
        int stepsElapsed = 0;
        boolean creepStarted = false;

        double currentInterval;
        double tickAccumulator = 0;

        CountdownTask(
                int blocksLeft,
                int baseDecrement,
                String tickSoundStr,
                String endSoundStr
        ) {
            this.blocksLeft = blocksLeft;

            this.currentInterval =
                    HorrorConfigManager.getConfig()
                            .countdown
                            .startInterval;

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

        if (registered) {
            return;
        }

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(CountdownEngine::tick);

        HorrorOperator.LOGGER.info(
                "[HorrorOperator] CountdownEngine registered."
        );
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

            int blocks = task.blocksLeft;
            if (blocks <= 50 && RANDOM.nextInt(80) == 0) {

                target.addEffect(
                        new MobEffectInstance(
                                MobEffects.DARKNESS,
                                30,
                                0,
                                false,
                                false,
                                true
                        )
                );
            }
            if (blocks <= 25 && RANDOM.nextInt(60) == 0) {

                target.addEffect(
                        new MobEffectInstance(
                                MobEffects.CONFUSION,
                                40,
                                0,
                                false,
                                false,
                                true
                        )
                );
            }
            // Accelerate countdown
            task.currentInterval =
                    Math.max(
                            HorrorConfigManager.getConfig()
                                    .countdown
                                    .minimumInterval,

                            task.currentInterval *
                                    HorrorConfigManager.getConfig()
                                            .countdown
                                            .speedupMultiplier
                    );

            if (blocks <= 40 && RANDOM.nextInt(100) == 0) {

                target.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§8<§f???§8> §7I see you."
                        )
                );
            }

            if (blocks <= 35 && RANDOM.nextInt(50) == 0) {

                target.playNotifySound(
                        SoundEvent.createVariableRangeEvent(
                                new net.minecraft.resources.ResourceLocation(
                                        "minecraft:block.stone.step"
                                )
                        ),
                        SoundSource.HOSTILE,
                        1.0f,
                        0.8f + (RANDOM.nextFloat() * 0.4f)
                );
            }

            if (task.blocksLeft <= 0) {

                if (task.endSound != null) {

                    target.playNotifySound(
                            SoundEvent.createVariableRangeEvent(task.endSound),
                            SoundSource.MASTER,
                            1.0f,
                            0.9f
                    );

                    target.playNotifySound(
                            SoundEvent.createVariableRangeEvent(
                                    new net.minecraft.resources.ResourceLocation(
                                            "minecraft:entity.elder_guardian.curse"
                                    )
                            ),
                            SoundSource.MASTER,
                            2.0f,
                            1.0f
                    );

                    target.playNotifySound(
                            SoundEvent.createVariableRangeEvent(
                                    new net.minecraft.resources.ResourceLocation(
                                            "minecraft:entity.warden.roar"
                                    )
                            ),
                            SoundSource.MASTER,
                            2.0f,
                            1.0f
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
                    task.blocksLeft <=
                            HorrorConfigManager.getConfig()
                                    .countdown
                                    .runThreshold) {

                task.creepStarted = true;

                HorrorNetworking.sendTo(
                        target,
                        new PanicScarePayload(
                                PanicScarePayload.CREEP_DISTORT
                        )
                );
            }else if (task.blocksLeft <= HorrorConfigManager.getConfig()
                    .countdown.runThreshold) {

                if (task.stepsElapsed % 3 == 0) {

                    target.playNotifySound(
                            SoundEvent.createVariableRangeEvent(
                                    new ResourceLocation(
                                            "minecraft:block.note_block.basedrum"
                                    )
                            ),
                            SoundSource.MASTER,
                            1.0f,
                            0.6f
                    );
                }
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

            int runThreshold =
                    HorrorConfigManager.getConfig()
                            .countdown
                            .runThreshold;

            if (task.blocksLeft <= runThreshold) {

                target.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("RUN"),
                        true
                );
            }
            else {

                target.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "It is " + task.blocksLeft + " blocks away."
                        ),
                        true
                );
            }

            if (task.blocksLeft <=
                    HorrorConfigManager.getConfig()
                            .countdown
                            .runThreshold) {

                HorrorNetworking.sendTo(
                        target,
                        new CountdownPayload(
                                task.blocksLeft,
                                false
                        )
                );
            }

            return false;
        });
    }
}