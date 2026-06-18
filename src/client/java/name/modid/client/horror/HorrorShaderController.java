package name.modid.client.horror;

import name.modid.client.mixin.GameRendererAccessor;
import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class HorrorShaderController {

    private static final ResourceLocation DISTORT_SHADER_ID =
            new ResourceLocation(
                    HorrorOperator.MOD_ID,
                    "shaders/post/horror_distort.json"
            );

    private static final long CREEP_DURATION_MS = 4500L;

    private static volatile boolean distortActive = false;
    private static volatile boolean creepActive = false;
    private static volatile long creepStartedAt = 0L;

    private static boolean registered = false;

    private HorrorShaderController() {}

    public static void register() {

        if (registered) {
            return;
        }

        registered = true;

        HudRenderCallback.EVENT.register(
                HorrorShaderController::onHudRender
        );

        HorrorOperator.LOGGER.info(
                "[HorrorOperator] HorrorShaderController registered."
        );
    }

    /**
     * Starts the creep phase.
     */
    public static void applyDistort() {
        startCreep();
    }

    public static void startCreep() {

        Minecraft mc = Minecraft.getInstance();

        mc.execute(() -> {

            if (distortActive) {
                return;
            }

            if (!creepActive) {
                creepStartedAt = System.currentTimeMillis();
            }

            creepActive = true;
        });
    }

    public static void clearShader() {

        Minecraft mc = Minecraft.getInstance();

        mc.execute(() -> {

            try {

                creepActive = false;

                if (distortActive
                        && mc.gameRenderer.currentEffect() != null) {

                    mc.gameRenderer.shutdownEffect();
                }

                distortActive = false;

                HorrorOperator.LOGGER.info(
                        "[HorrorOperator] Horror distort shader unloaded."
                );

            } catch (Exception e) {

                creepActive = false;
                distortActive = false;

                HorrorOperator.LOGGER.error(
                        "[HorrorOperator] Failed to clear shader: {}",
                        e.getMessage(),
                        e
                );
            }
        });
    }

    public static boolean isDistortActive() {
        return distortActive;
    }

    public static void markInactive() {
        creepActive = false;
        distortActive = false;
    }

    private static void onHudRender(
            GuiGraphics graphics,
            float tickDelta
    ) {

        if (!creepActive || distortActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            creepActive = false;
            return;
        }

        long elapsed =
                System.currentTimeMillis() - creepStartedAt;

        float progress =
                Math.min(
                        1.0f,
                        elapsed / (float) CREEP_DURATION_MS
                );

        /*
         * Faster heartbeat pulse.
         */
        float pulse =
                0.5f +
                        0.5f *
                                (float)Math.sin(elapsed / 55.0);

        int redAlpha =
                Math.min(
                        120,
                        (int)(progress * (40 + pulse * 80))
                );

        int blackAlpha =
                Math.min(
                        180,
                        (int)(progress * progress * 180)
                );

        int width =
                mc.getWindow().getGuiScaledWidth();

        int height =
                mc.getWindow().getGuiScaledHeight();

        /*
         * Main red tint.
         */
        graphics.fill(
                0,
                0,
                width,
                height,
                (redAlpha << 24) | 0x330000
        );

        /*
         * Darkness closing in.
         */
        graphics.fill(
                0,
                0,
                width,
                height,
                blackAlpha << 24
        );

        /*
         * Heartbeat flash.
         */
        if (pulse > 0.92f) {

            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    0x22AA0000
            );
        }

        /*
         * Random white flickers.
         */
        if (Math.random() < (progress * 0.06f)) {

            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    0x55FFFFFF
            );
        }

        /*
         * Blackout flashes near the end.
         */
        if (progress > 0.75f
                && Math.random() < 0.01f) {

            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    0xCC000000
            );
        }

        /*
         * Camera paranoia effect.
         */
        if (progress > 0.5f) {

            float shake =
                    (float)Math.sin(elapsed / 25.0)
                            * progress
                            * 0.25f;

            mc.player.setYRot(
                    mc.player.getYRot() + shake
            );
        }

        /*
         * End creep phase.
         */
        if (progress >= 1.0f) {
            loadDistortNow();
        }
    }

    private static void loadDistortNow() {

        Minecraft mc = Minecraft.getInstance();

        if (!mc.isSameThread()) {
            mc.execute(HorrorShaderController::loadDistortNow);
            return;
        }

        if (distortActive
                && mc.gameRenderer.currentEffect() != null) {

            creepActive = false;
            return;
        }

        try {

            ((GameRendererAccessor) mc.gameRenderer)
                    .invokeLoadEffect(DISTORT_SHADER_ID);

            if (mc.gameRenderer.currentEffect() != null) {

                distortActive = true;
                creepActive = false;

                HorrorOperator.LOGGER.info(
                        "[HorrorOperator] Horror distort shader loaded."
                );

            } else {

                distortActive = false;
                creepActive = false;

                HorrorOperator.LOGGER.error(
                        "[HorrorOperator] Horror distort shader failed to load (no active effect)."
                );
            }

        } catch (Exception e) {

            distortActive = false;
            creepActive = false;

            HorrorOperator.LOGGER.error(
                    "[HorrorOperator] Failed to load distort shader: {}",
                    e.getMessage(),
                    e
            );
        }
    }
}