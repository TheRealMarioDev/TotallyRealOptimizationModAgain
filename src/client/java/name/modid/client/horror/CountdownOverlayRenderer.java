package name.modid.client.horror;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Reserved for future horror visual effects.
 *
 * Countdown messaging now uses the action bar instead of
 * center-screen HUD text.
 */
public final class CountdownOverlayRenderer {

    private CountdownOverlayRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            // Future:
            // - static flashes
            // - vignette pulses
            // - silhouette flashes
            // - fake Herobrine sightings
        });
    }

    public static void update(int blocks, boolean finalTick) {
        // No-op for now.
    }

    public static void clear() {
        // No-op for now.
    }
}