package name.modid.horror.spy;

import name.modid.horror.HorrorOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Feature 2 — Inventory spy action payload (C2S).
 *
 * <p>Sent from the admin's client to the server when they interact with the spy screen
 * (move or delete an item). The server applies the action to the <em>target</em> player's
 * live inventory without triggering any animation or sound on the target's side.</p>
 *
 * <h3>Action types</h3>
 * <ul>
 *   <li>{@link #ACTION_SET} (0) — Set slot {@code slotIndex} to {@code stack}</li>
 *   <li>{@link #ACTION_DELETE} (1) — Clear slot {@code slotIndex} (stack is {@link ItemStack#EMPTY})</li>
 *   <li>{@link #ACTION_CLOSE} (2) — Admin closed the spy screen; server stops syncing</li>
 * </ul>
 */
public record SpyActionPayload(int action, int slotIndex, ItemStack stack, String targetName)
        implements CustomPacketPayload {

    public static final int ACTION_SET    = 0;
    public static final int ACTION_DELETE = 1;
    public static final int ACTION_CLOSE  = 2;

    public static final ResourceLocation ID = new ResourceLocation(HorrorOperator.MOD_ID, "spy_action");
    public static final CustomPacketPayload.Type<SpyActionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SpyActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,                    SpyActionPayload::action,
                    ByteBufCodecs.VAR_INT,                    SpyActionPayload::slotIndex,
                    // OPTIONAL_STREAM_CODEC handles ItemStack.EMPTY safely
                    ItemStack.OPTIONAL_STREAM_CODEC,          SpyActionPayload::stack,
                    ByteBufCodecs.STRING_UTF8,                SpyActionPayload::targetName,
                    SpyActionPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
