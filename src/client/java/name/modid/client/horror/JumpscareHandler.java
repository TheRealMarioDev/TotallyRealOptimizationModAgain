package name.modid.client.horror;

import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-side jumpscare burst: loud layered sounds + brief white flash when the countdown hits zero.
 */
public final class JumpscareHandler {

    private static volatile float flashAlpha = 0f;
    private static boolean registered = false;

    private JumpscareHandler() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudRenderCallback.EVENT.register(JumpscareHandler::onHudRender);
        HorrorOperator.LOGGER.info("[HorrorOperator] JumpscareHandler registered.");
    }

    /** Fire the jumpscare burst (safe from any thread). */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(JumpscareHandler::playBurst);
    }

    private static void playBurst() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        flashAlpha = 1.0f;

        play(mc, "minecraft:entity.warden.sonic_boom", 0.85f);
        play(mc, "minecraft:entity.wither.spawn", 0.6f);
        play(mc, "minecraft:entity.generic.explode", 0.55f);
        play(mc, "minecraft:entity.enderman.scream", 0.45f);
        play(mc, "minecraft:block.anvil.land", 0.35f);
    }

    private static void play(Minecraft mc, String soundId, float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(new ResourceLocation(soundId));
        mc.player.playSound(event, 1.0f, pitch);
    }

    private static void onHudRender(GuiGraphics graphics, float tickDelta) {
        if (flashAlpha <= 0.01f) {
            flashAlpha = 0f;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int alpha = Math.min(255, (int) (flashAlpha * 255));
        graphics.fill(0, 0, w, h, (alpha << 24) | 0xFFFFFF);
        flashAlpha *= 0.82f;
    }
}
