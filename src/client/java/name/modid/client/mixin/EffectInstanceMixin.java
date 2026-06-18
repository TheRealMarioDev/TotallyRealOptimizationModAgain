package name.modid.client.mixin;

import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla post-processing only resolves program JSON and stage files from the
 * {@code minecraft} namespace. This mixin rewrites namespaced pass references such as
 * {@code optimizationmod:horror_pixelate} to {@code optimizationmod:shaders/program/horror_pixelate.json}.
 */
@Mixin(EffectInstance.class)
public abstract class EffectInstanceMixin {

    private static final String PROGRAM_PREFIX = "shaders/program/";

    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;)V"),
            index = 0
    )
    private String horror$rewriteProgramJsonPath(String path) {
        return rewriteNamespacedShaderPath(path);
    }

    @ModifyArg(
            method = "getOrCreate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;)V"),
            index = 0
    )
    private static String horror$rewriteShaderStagePath(String path) {
        return rewriteNamespacedShaderPath(path);
    }

    /**
     * Converts {@code shaders/program/modid:shader.json} into {@code modid:shaders/program/shader.json}.
     */
    private static String rewriteNamespacedShaderPath(String path) {
        int nameStart = path.indexOf(PROGRAM_PREFIX);
        if (nameStart < 0) {
            return path;
        }
        nameStart += PROGRAM_PREFIX.length();
        int nameEnd = path.lastIndexOf('.');
        if (nameEnd <= nameStart) {
            return path;
        }

        String containedId = path.substring(nameStart, nameEnd);
        if (!containedId.contains(":")) {
            return path;
        }

        ResourceLocation contained = new ResourceLocation(containedId);
        return contained.getNamespace() + ":" + path.replace(containedId, contained.getPath());
    }
}
