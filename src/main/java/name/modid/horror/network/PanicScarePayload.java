package name.modid.horror.network;

import name.modid.horror.HorrorOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Feature 1 — Modern 1.20.6 networking pipeline.
 *
 * <p>A {@link CustomPacketPayload} carrying a single integer "scare intensity / type".
 * This is the modern replacement for the pre-1.20.2 {@code PacketByteBuf} + raw channel
 * registration approach. The payload is registered against {@code PayloadTypeRegistry}
 * (see {@link HorrorNetworking}) so that both the server (sender) and the client
 * (receiver) agree on the wire format via the shared {@link #STREAM_CODEC}.</p>
 *
 * <p>Intensity semantics (kept as named constants so command code and the client
 * receiver never disagree on the magic numbers):</p>
 * <ul>
 *     <li>{@link #RESET} (0)   — safely tear down all client-side scare state
 *         (unloads the post-processing shader, clears the countdown overlay).</li>
 *     <li>{@link #COUNTDOWN} (1) — arm the "Herobrine is closing in" ambiance on the client.</li>
 *     <li>{@link #TOTAL_DISTORT} (2) — load the high-intensity post-processing shader.</li>
 * </ul>
 */
public record PanicScarePayload(int intensity) implements CustomPacketPayload {

    /** Scare types. Shared by both logical sides so they can never drift apart. */
    public static final int RESET = 0;
    public static final int COUNTDOWN = 1;
    public static final int TOTAL_DISTORT = 2;
    public static final int CREEP_DISTORT = 3;

    /** Stable channel id. Namespaced under the mod id. */
    public static final ResourceLocation ID = new ResourceLocation(HorrorOperator.MOD_ID, "panic_scare");

    /** The payload type token used to register and dispatch this packet. */
    public static final CustomPacketPayload.Type<PanicScarePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    /**
     * Wire codec. {@code ByteBufCodecs.VAR_INT} is {@code StreamCodec<ByteBuf, Integer>};
     * because {@code RegistryFriendlyByteBuf} extends {@code FriendlyByteBuf} (and thus
     * {@code ByteBuf}), {@code composite} happily infers the registry buffer type that
     * {@code PayloadTypeRegistry.playS2C()} requires.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PanicScarePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PanicScarePayload::intensity,
                    PanicScarePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
