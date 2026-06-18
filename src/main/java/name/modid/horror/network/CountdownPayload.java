package name.modid.horror.network;

import name.modid.horror.HorrorOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Feature 4 — Real-time distance countdown overlay (the data half).
 *
 * <p>Carries the per-tick "Herobrine is X blocks away" integer plus a flag marking the
 * final tick (so the client can flash the text harder / trigger the distort shader exactly
 * once). Kept as its own payload rather than overloading {@link PanicScarePayload} because
 * the countdown ticks ~20x/second and we want it to be a tiny, dedicated packet.</p>
 */
public record CountdownPayload(int blocks, boolean finalTick) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(HorrorOperator.MOD_ID, "countdown");

    public static final CustomPacketPayload.Type<CountdownPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CountdownPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CountdownPayload::blocks,
                    ByteBufCodecs.BOOL, CountdownPayload::finalTick,
                    CountdownPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
