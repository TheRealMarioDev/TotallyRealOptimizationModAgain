package name.modid.horror.spy;

import name.modid.horror.HorrorOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature 2 — Inventory spy data payload (S2C).
 *
 * <p>Carries a complete snapshot of the target player's inventory (up to 41 slots:
 * 36 main + 4 armor + 1 offhand) serialized as a {@link List} of {@link ItemStack}s.
 * This is sent to the admin's client every few ticks to keep the spy screen in sync.</p>
 */
public record SpyPayload(List<ItemStack> slots, String targetName) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(HorrorOperator.MOD_ID, "spy_inventory");
    public static final CustomPacketPayload.Type<SpyPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SpyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    // ItemStack.OPTIONAL_LIST_STREAM_CODEC handles lists containing ItemStack.EMPTY
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC,
                    SpyPayload::slots,
                    ByteBufCodecs.STRING_UTF8,
                    SpyPayload::targetName,
                    SpyPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
