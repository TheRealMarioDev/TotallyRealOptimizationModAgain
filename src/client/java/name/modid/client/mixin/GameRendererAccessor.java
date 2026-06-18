package name.modid.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin interface to invoke the private {@link GameRenderer#loadEffect(ResourceLocation)} method.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("loadEffect")
    void invokeLoadEffect(ResourceLocation id);
}
