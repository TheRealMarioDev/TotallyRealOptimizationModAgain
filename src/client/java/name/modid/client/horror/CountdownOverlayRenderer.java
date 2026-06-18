package name.modid.client.horror;

import name.modid.horror.HorrorOperator;
import name.modid.horror.countdown.CountdownEngine;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Countdown HUD: distance phase, then a simple flashing RUN prompt.
 */
public final class CountdownOverlayRenderer {

    private static volatile int blocksAway = 0;
    private static volatile boolean active = false;
    private static volatile boolean finalTick = false;
    private static boolean registered = false;

    private CountdownOverlayRenderer() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudRenderCallback.EVENT.register(CountdownOverlayRenderer::onHudRender);
        HorrorOperator.LOGGER.info("[HorrorOperator] CountdownOverlayRenderer registered.");
    }

    public static void update(int blocks, boolean isFinal) {
        blocksAway = Math.max(0, blocks);
        finalTick = isFinal;
        active = true;
        if (isFinal) {
            JumpscareHandler.trigger();
        }
    }

    public static void clear() {
        active = false;
        finalTick = false;
        blocksAway = 0;
    }

    private static void onHudRender(GuiGraphics graphics, float tickDelta) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            active = false;
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int y = (int) (screenHeight * 0.68f);

        boolean flashOn = (System.currentTimeMillis() / 100L) % 2 == 0;
        boolean runPhase = blocksAway <= CountdownEngine.RUN_THRESHOLD;

        String message;
        int textColor;

        if (finalTick || runPhase) {
            message = "§c§lRUN.";
            textColor = flashOn ? 0xFF2222 : 0xAA0000;
        } else {
            message = flashOn
                    ? "§4Herobrine is §c§l" + blocksAway + " §4blocks away"
                    : "§cHerobrine is §4§l" + blocksAway + " §cblocks away";
            textColor = flashOn ? 0xAA0000 : 0xFF4444;
        }

        float scale = runPhase || finalTick ? 1.35f : 1.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(screenWidth / 2f, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawCenteredString(mc.font, Component.literal(message), 0, 0, textColor);
        graphics.pose().popPose();
    }
}
