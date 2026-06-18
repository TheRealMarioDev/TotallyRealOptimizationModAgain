package name.modid.horror.mixin;

import name.modid.horror.vanish.VanishManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 2 — Silent block interaction for vanished admins.
 *
 * <p>Mixes into {@link ServerLevel#levelEvent(int, BlockPos, int)}, which is the main
 * broadcast method for world-level events sent to nearby clients. Block-break particles and
 * sounds ({@code LevelEvent.PARTICLES_DESTROY_BLOCK}, event id 2001) pass through this
 * method.</p>
 *
 * <p>When this is called while the vanished admin is the most recent interaction source
 * (tracked via our {@link VanishManager#setLastInteractingPlayer(ServerPlayer)} hook called
 * from the destroy-block pipeline), we cancel the broadcast.</p>
 *
 * <p><strong>Implementation note:</strong> {@code levelEvent} does not carry a "who caused
 * this?" parameter, so we use a thread-local last-actor pattern — {@link VanishManager}
 * stores the last player who broke/placed a block on the current tick and
 * {@link #horror$suppressVanishedLevelEvent(net.minecraft.world.entity.player.Player, int, BlockPos, int, CallbackInfo)}
 * the overload that <em>does</em> carry the player is what we actually intercept here.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVanishMixin {

    /**
     * Intercepts the player-identified overload of {@code levelEvent} that is used when a
     * {@link ServerPlayer} directly causes a block event (e.g., breaks a block). If the
     * causing player is vanished we cancel the broadcast entirely — no block-break particles,
     * no block-break sound.
     *
     * <p>The vanilla method signature is:
     * {@code levelEvent(@Nullable Player player, int type, BlockPos pos, int data)}.
     * Mixin resolves the correct overload automatically via descriptor matching.</p>
     */
    @Inject(
            method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void horror$suppressVanishedLevelEvent(
            net.minecraft.world.entity.player.Player player,
            int type,
            BlockPos pos,
            int data,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer sp && VanishManager.isVanished(sp)) {
            // Cancel the levelEvent broadcast — no particles, no sounds reach other clients
            ci.cancel();
        }
    }

}

