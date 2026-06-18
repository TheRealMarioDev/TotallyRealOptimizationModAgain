package name.modid.horror.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Feature 2 — Exposes the private {@code entity} field of {@link ServerEntity} so the
 * tracked-entity mixin can ask "which entity does this tracker belong to?".
 */
@Mixin(ServerEntity.class)
public interface ServerEntityAccessor {
    @Accessor("entity")
    Entity horror$getEntity();
}
