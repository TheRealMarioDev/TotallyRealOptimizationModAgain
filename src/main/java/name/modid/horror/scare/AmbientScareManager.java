package name.modid.horror.scare;

import name.modid.horror.HorrorOperator;
import name.modid.horror.fromthefog.FogIntegration;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.network.PanicScarePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timed, believable Herobrine-style scares that are stronger with From the Fog,
 * but still work as standalone server-side effects.
 */
public final class AmbientScareManager {

    private static final Map<UUID, StalkTask> ACTIVE_STALKS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private AmbientScareManager() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(AmbientScareManager::tick);
        HorrorOperator.LOGGER.info("[HorrorOperator] AmbientScareManager registered.");
    }

    public static void reset(ServerPlayer target, MinecraftServer server) {
        ACTIVE_STALKS.remove(target.getUUID());
        if (FogIntegration.isFtfLoaded()) {
            FogIntegration.clearHerobrinePresence(target, server);
        }
    }

    public static void whisper(ServerPlayer target, String message) {
        target.sendSystemMessage(Component.literal("§4§l"+message));
        play(target, "minecraft:entity.enderman.ambient", SoundSource.HOSTILE, 0.55f, 0.55f);
    }

    public static void omen(ServerPlayer target, MinecraftServer server) {
        if (target.level() instanceof ServerLevel level) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                BlockPos pos = target.blockPosition().relative(target.getDirection().getOpposite(), 7);
                bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }

        target.displayClientMessage(Component.literal("§4You should not have looked."), true);
        play(target, "minecraft:entity.lightning_bolt.thunder", SoundSource.WEATHER, 1.0f, 0.75f);
        play(target, "minecraft:entity.wither.ambient", SoundSource.HOSTILE, 0.8f, 0.45f);
        play(target, "minecraft:block.respawn_anchor.deplete", SoundSource.BLOCKS, 0.9f, 0.8f);

        if (FogIntegration.isFtfLoaded()) {
            FogIntegration.applyHerobrinePresence(target, server, 1);
        }
    }

    public static boolean mark(ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos center = findMarkCenter(target, level);
        if (center == null) {
            return false;
        }

        placeIfAir(level, center, Blocks.SOUL_SAND.defaultBlockState());
        placeIfAir(level, center.above(), Blocks.NETHERRACK.defaultBlockState());
        placeIfAir(level, center.above(2), Blocks.REDSTONE_TORCH.defaultBlockState());
        placeIfAir(level, center.north(), Blocks.NETHERRACK.defaultBlockState());
        placeIfAir(level, center.south(), Blocks.NETHERRACK.defaultBlockState());
        placeIfAir(level, center.east(), Blocks.NETHERRACK.defaultBlockState());
        placeIfAir(level, center.west(), Blocks.NETHERRACK.defaultBlockState());
        placeIfAir(level, center.north().above(), Blocks.REDSTONE_TORCH.defaultBlockState());
        placeIfAir(level, center.south().above(), Blocks.REDSTONE_TORCH.defaultBlockState());
        placeIfAir(level, center.east().above(), Blocks.REDSTONE_TORCH.defaultBlockState());
        placeIfAir(level, center.west().above(), Blocks.REDSTONE_TORCH.defaultBlockState());

        play(target, "minecraft:block.portal.ambient", SoundSource.AMBIENT, 0.9f, 0.55f);
        target.displayClientMessage(Component.literal("§8Something was built behind you."), true);
        return true;
    }

    public static void stalk(ServerPlayer target, MinecraftServer server, int seconds) {
        int clampedSeconds = Math.max(5, Math.min(180, seconds));
        ACTIVE_STALKS.put(target.getUUID(), new StalkTask(clampedSeconds * 20));
        if (FogIntegration.isFtfLoaded()) {
            FogIntegration.applyHerobrinePresence(target, server, 20);
        }
        target.displayClientMessage(Component.literal("§8You feel watched."), true);
        play(target, "minecraft:ambient.cave", SoundSource.AMBIENT, 0.8f, 0.6f);
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE_STALKS.isEmpty()) return;

        ACTIVE_STALKS.entrySet().removeIf(entry -> {
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            if (target == null) return true;

            StalkTask task = entry.getValue();
            task.ticksRemaining--;
            task.ticksElapsed++;

            if (task.ticksElapsed >= task.nextEventTick) {
                fireStalkBeat(target, server, task);
                task.nextEventTick += Math.max(12, 34 - task.beats * 3);
                task.beats++;
            }

            if (task.ticksRemaining <= 0) {
                finishStalk(target, server);
                return true;
            }

            return false;
        });
    }

    private static void fireStalkBeat(ServerPlayer target, MinecraftServer server, StalkTask task) {
        int distance = Math.max(1, 18 - task.beats * 2);
        if (FogIntegration.isFtfLoaded()) {
            FogIntegration.applyHerobrinePresence(target, server, distance);
        }

        switch (task.beats % 6) {
            case 0 -> {
                play(target, "minecraft:block.wooden_door.open", SoundSource.BLOCKS, 0.55f, 0.5f);
                target.displayClientMessage(Component.literal("§8A door opens somewhere."), true);
            }
            case 1 -> play(target, "minecraft:block.gravel.step", SoundSource.BLOCKS, 0.9f, 0.45f);
            case 2 -> play(target, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.45f, 0.65f);
            case 3 -> {
                whisper(target, "turn around");
                HorrorNetworking.sendTo(target, new PanicScarePayload(PanicScarePayload.COUNTDOWN));
            }
            case 4 -> play(target, "minecraft:block.scaffolding.step", SoundSource.BLOCKS, 0.8f, 0.5f);
            default -> play(target, "minecraft:ambient.cave", SoundSource.AMBIENT, 0.7f, 0.75f);
        }
    }

    private static void finishStalk(ServerPlayer target, MinecraftServer server) {
        whisper(target, "I found you");
        omen(target, server);
        HorrorNetworking.sendTo(target, new PanicScarePayload(PanicScarePayload.TOTAL_DISTORT));
    }

    private static BlockPos findMarkCenter(ServerPlayer target, ServerLevel level) {
        BlockPos start = target.blockPosition().relative(target.getDirection().getOpposite(), 6);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos floor = start.offset(0, dy, 0);
            BlockPos center = floor.above();
            if (!level.getBlockState(floor).isAir()
                    && level.isEmptyBlock(center)
                    && level.isEmptyBlock(center.above())
                    && level.isEmptyBlock(center.above(2))) {
                return center;
            }
        }
        return null;
    }

    private static void placeIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.isEmptyBlock(pos)) {
            level.setBlockAndUpdate(pos, state);
        }
    }

    private static void play(ServerPlayer target, String soundId, SoundSource source, float volume, float pitch) {
        target.playNotifySound(
                SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(soundId)),
                source,
                volume,
                pitch
        );
    }

    private static final class StalkTask {
        int ticksRemaining;
        int ticksElapsed;
        int nextEventTick = 10;
        int beats;

        StalkTask(int ticksRemaining) {
            this.ticksRemaining = ticksRemaining;
        }
    }
}
