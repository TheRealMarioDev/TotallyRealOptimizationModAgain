package name.modid.horror.mixin;

import name.modid.horror.vanish.VanishManager;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 2 — Entity tracker vanish enforcement.
 *
 * <p>Mixes into {@link ServerEntity#addPairing(ServerPlayer)}, which is the internal method
 * that the entity tracking system calls to "subscribe" a given client to receiving updates
 * (spawn packet, position updates, metadata) for a particular entity.</p>
 *
 * <p>By returning early (cancelling the callback) when the tracked entity is a vanished admin,
 * we ensure:</p>
 * <ul>
 *   <li>No spawn packet is ever sent to non-admin clients for the vanished player.</li>
 *   <li>No movement updates are sent → no footstep sounds tied to entity movement.</li>
 *   <li>No entity-attached particles (equipment enchantment glow, etc.) are rendered.</li>
 * </ul>
 *
 * <p>The {@link ServerEntityAccessor} interface is used to read the private {@code entity}
 * field from the {@link ServerEntity} instance without reflection.</p>
 */
@Mixin(ServerEntity.class)
public abstract class TrackedEntityVanishMixin {

    /**
     * Injected at the HEAD of {@code addPairing}. If the entity being paired is a vanished
     * admin and the pairing target is a different (non-admin) player, we cancel the call so
     * that no tracking data is sent.
     *
     * @param player the client being added to the tracking pair — i.e., the viewer
     * @param ci     the callback info used to cancel
     */
    @Inject(method = "addPairing", at = @At("HEAD"), cancellable = true)
    private void horror$suppressVanishedPairing(ServerPlayer player, CallbackInfo ci) {
        // Retrieve the entity this ServerEntity tracker belongs to
        Entity trackedEntity = ((ServerEntityAccessor) this).horror$getEntity();

        // Only act if the tracked entity is itself a player (admin candidate)
        if (!(trackedEntity instanceof ServerPlayer trackedPlayer)) return;

        // If the tracked player is vanished AND the viewer is not the vanished player themselves
        // (the admin should still see their own entity in first-person edge cases), cancel.
        if (VanishManager.isVanished(trackedPlayer) && !trackedPlayer.getUUID().equals(player.getUUID())) {
            ci.cancel();
        }
    }
}
