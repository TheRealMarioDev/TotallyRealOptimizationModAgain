package name.modid.client;

import name.modid.client.horror.CountdownOverlayRenderer;
import name.modid.client.horror.HorrorShaderController;
import name.modid.client.horror.JumpscareHandler;
import name.modid.client.horror.SpyInventoryScreen;
import name.modid.horror.network.CountdownPayload;
import name.modid.horror.network.PanicScarePayload;
import name.modid.horror.spy.SpyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client-side mod entry point.
 *
 * <p>Registers all client-facing network handlers and the HUD overlay renderer.
 * This class runs ONLY on the client — no server-side logic lives here.</p>
 */
public class OptimizationModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // ----------------------------------------------------------------
        // Feature 4 — Countdown overlay renderer registration
        // ----------------------------------------------------------------
        CountdownOverlayRenderer.register();
        JumpscareHandler.register();
        HorrorShaderController.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                HorrorShaderController.markInactive()
        );

        // ----------------------------------------------------------------
        // Feature 1 & 5 — PanicScarePayload client handler
        //
        // Dispatches on intensity:
        //   RESET (0)        → clear shader + overlay
        //   COUNTDOWN (1)    → arm the overlay display (actual numbers come via CountdownPayload)
        //   TOTAL_DISTORT(2) → load the horror distort post-processing shader
        // ----------------------------------------------------------------
        ClientPlayNetworking.registerGlobalReceiver(PanicScarePayload.TYPE, (payload, context) -> {
            switch (payload.intensity()) {
                case PanicScarePayload.RESET -> {
                    // Clear the overlay and safely unload the post-processor
                    CountdownOverlayRenderer.clear();
                    HorrorShaderController.clearShader();
                }
                case PanicScarePayload.COUNTDOWN -> {
                    // "Arm" state — the overlay activates on the first CountdownPayload tick
                    // Nothing to do here; we keep it for future use / direct arming.
                }
                case PanicScarePayload.TOTAL_DISTORT -> {
                    HorrorShaderController.applyDistort();
                    CountdownOverlayRenderer.clear();
                }
                case PanicScarePayload.CREEP_DISTORT -> {
                    HorrorShaderController.startCreep();
                }
            }
        });

        // ----------------------------------------------------------------
        // Feature 4 — CountdownPayload client handler
        //
        // Updates the overlay with the current block distance every server tick.
        // ----------------------------------------------------------------
        ClientPlayNetworking.registerGlobalReceiver(CountdownPayload.TYPE, (payload, context) -> {
            CountdownOverlayRenderer.update(payload.blocks(), payload.finalTick());
        });

        // ----------------------------------------------------------------
        // Feature 2 — SpyPayload client handler
        //
        // Opens or refreshes the spy inventory screen on the Minecraft render thread.
        // ----------------------------------------------------------------
        ClientPlayNetworking.registerGlobalReceiver(SpyPayload.TYPE, (payload, context) -> {
            // Screen operations must run on the main client thread
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> SpyInventoryScreen.openOrRefresh(payload.targetName(), payload.slots()));
        });
    }
}
