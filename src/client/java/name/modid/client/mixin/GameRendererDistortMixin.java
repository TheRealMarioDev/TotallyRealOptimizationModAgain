package name.modid.client.mixin;

import name.modid.client.horror.HorrorShaderController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-applies the horror post effect if a resource reload flushed it while still armed.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererDistortMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void horror$checkDistortConsistency(
            float partialTick,
            long finishNanoTime,
            CallbackInfo ci
    ) {
        if (HorrorShaderController.isDistortActive()) {
            GameRenderer self = (GameRenderer) (Object) this;
            if (self.currentEffect() == null) {
                HorrorShaderController.applyDistort();
            }
        }
    }
}
