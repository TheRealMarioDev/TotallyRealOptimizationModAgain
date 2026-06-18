package name.modid.horror.curse;

import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Feature 3 — Truly Permanent Curse engine (server-side).
 *
 * <p>The "Herobrine Curse" is stored via Fabric's {@link AttachmentType} system, which
 * persists data on player entities across dimensions and (via serialization) across
 * server restarts. In 1.20.6 {@code ServerPlayer.getPersistentData()} no longer exists —
 * the recommended approach is Fabric's Data Attachments API.</p>
 */
public final class CurseManager {

    /**
     * Fabric attachment type that marks a player as Herobrine-cursed.
     * {@code persistent = true} means the value is serialized to the player's save file
     * and survives server restarts.
     */
    public static final AttachmentType<Boolean> CURSE_ATTACHMENT =
            AttachmentRegistry.<Boolean>builder()
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .initializer(() -> false)
                    .copyOnDeath()
                    .buildAndRegister(new ResourceLocation(HorrorOperator.MOD_ID, "herobrine_curse"));

    private CurseManager() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Flags the target with the Herobrine Curse and immediately applies permanent effects.
     */
    public static void flagWithCurse(ServerPlayer target) {
        target.setAttached(CURSE_ATTACHMENT, true);
        applyEffects(target);
        HorrorOperator.LOGGER.info("[HorrorOperator] Herobrine Curse applied to {}.", target.getGameProfile().getName());
    }

    /**
     * Removes the Herobrine Curse flag and clears both darkness and blindness effects.
     */
    public static void clearCurse(ServerPlayer target) {
        target.setAttached(CURSE_ATTACHMENT, false);
        target.removeEffect(MobEffects.DARKNESS);
        target.removeEffect(MobEffects.BLINDNESS);
        HorrorOperator.LOGGER.info("[HorrorOperator] Herobrine Curse cleared from {}.", target.getGameProfile().getName());
    }

    /**
     * @return {@code true} if the player currently carries the Herobrine Curse.
     */
    public static boolean isCursed(ServerPlayer player) {
        Boolean val = player.getAttached(CURSE_ATTACHMENT);
        return val != null && val;
    }

    /**
     * Re-applies permanent darkness and blindness effects after a respawn.
     */
    public static void reapplyCurseEffects(ServerPlayer player) {
        applyEffects(player);
        HorrorOperator.LOGGER.info("[HorrorOperator] Herobrine Curse re-applied to {} after respawn.", player.getGameProfile().getName());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static void applyEffects(ServerPlayer player) {
        // Darkness: amplifier 1 gives the maximum darkening sphere radius
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                Integer.MAX_VALUE,
                1,
                true,   // ambient — no particles
                false,  // visible — no swirl particles
                false   // showIcon — hidden from HUD
        ));

        // Blindness: solid black vignette overlay
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                Integer.MAX_VALUE,
                0,
                true,
                false,
                false
        ));
    }
}
