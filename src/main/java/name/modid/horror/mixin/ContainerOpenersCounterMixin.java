package name.modid.horror.mixin;

import name.modid.horror.vanish.VanishManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;

/**
 * Feature 2 — Silent chest/barrel/shulker open &amp; close for vanished admins.
 *
 * <p>{@link ContainerOpenersCounter} is the shared utility used by chest, barrel, and shulker
 * block entities to:</p>
 * <ol>
 *     <li>Track how many players have the container open (so the lid stays up while
 *         anyone is looking inside).</li>
 *     <li>Broadcast a {@code levelEvent} for the open/close "creak" sound and lid animation
 *         packet to nearby players.</li>
 * </ol>
 *
 * <p>We inject at the HEAD of both {@code incrementOpeners} and {@code decrementOpeners}.
 * If the player causing the open/close is a vanished admin we cancel the call. This prevents:</p>
 * <ul>
 *   <li>The chest-open sound being broadcast to nearby non-admin clients.</li>
 *   <li>The lid-rise animation being sent — other players will not see the chest open.</li>
 *   <li>The admin being counted toward the "openers" tally, so the lid won't accidentally
 *       stay open after the admin closes the chest.</li>
 * </ul>
 */
@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {

    /**
     * Suppress the chest-open sound and lid animation for vanished admins.
     *
     * @param player    the player opening the container
     * @param level     the level the container is in
     * @param pos       the block position of the container
     * @param container the container block entity
     * @param ci        callback info — cancelled if the opener is vanished
     */
    @Inject(method = "incrementOpeners", at = @At("HEAD"), cancellable = true)
    private void horror$suppressOpenSound(
            Player player,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer sp && VanishManager.isVanished(sp)) {
            ci.cancel();
        }
    }

    /**
     * Suppress the chest-close sound and lid-lower animation for vanished admins.
     *
     * @param player    the player closing the container
     * @param level     the level the container is in
     * @param pos       the block position of the container
     * @param container the container block entity
     * @param ci        callback info — cancelled if the closer is vanished
     */
    @Inject(method = "decrementOpeners", at = @At("HEAD"), cancellable = true)
    private void horror$suppressCloseSound(
            Player player,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer sp && VanishManager.isVanished(sp)) {
            ci.cancel();
        }
    }
}
