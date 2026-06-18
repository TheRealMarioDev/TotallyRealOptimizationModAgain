package name.modid.horror.curse;

import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

/**
 * Feature 3 — Permanent Curse respawn interception.
 *
 * <p>Registers Fabric's {@link ServerPlayerEvents#COPY_FROM} event, which fires when a new
 * {@link net.minecraft.server.level.ServerPlayer} instance is created to replace an old one
 * (i.e., death &amp; respawn, or server-side dimension transfer).</p>
 *
 * <p>When this fires, we:</p>
 * <ol>
 *     <li>Check if the player carries the Herobrine Curse (automatically copied by .copyOnDeath()).</li>
 *     <li>If yes, call {@link CurseManager#reapplyCurseEffects(net.minecraft.server.level.ServerPlayer)}
 *         so that the darkness and blindness are live before the player's first tick back in the world.</li>
 * </ol>
 *
 * <p>This makes death an ineffective escape: the effects are back before the respawn animation
 * completes.</p>
 */
public final class CurseRespawnHandler {

    private CurseRespawnHandler() {}

    /**
     * Registers the COPY_FROM callback. Must be called from the common mod initializer
     * ({@link name.modid.OptimizationMod#onInitialize()}).
     */
    public static void register() {
        // Run AFTER respawn so vanilla logic doesn't wipe our newly-added effects
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (CurseManager.isCursed(newPlayer)) {
                CurseManager.reapplyCurseEffects(newPlayer);
            }
        });

        // Run AFTER changing dimensions
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (CurseManager.isCursed(player)) {
                CurseManager.reapplyCurseEffects(player);
            }
        });

        HorrorOperator.LOGGER.info("[HorrorOperator] CurseRespawnHandler registered.");
    }
}
