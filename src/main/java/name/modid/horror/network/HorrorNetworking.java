package name.modid.horror.network;

import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Feature 1 — Central registration for the modern networking pipeline.
 *
 * <p>"Register the custom payload on both ServerPlayNetworking and ClientPlayNetworking":
 * the payload <em>type</em> itself is registered exactly once per logical flow direction
 * via {@link PayloadTypeRegistry} — this is what teaches both ends how to (de)serialize the
 * packet. The <em>receiving</em> side then attaches a handler:</p>
 * <ul>
 *     <li>Server &rarr; Client (S2C): {@link PayloadTypeRegistry#playS2C()} here, and the
 *     client attaches its handlers in {@code HorrorOperatorClient} via {@code ClientPlayNetworking}.</li>
 *     <li>Client &rarr; Server (C2S): {@link PayloadTypeRegistry#playC2S()} here, with a
 *     server handler attached via {@code ServerPlayNetworking} (currently unused by the
 *     admin flow, but registered so the channel is symmetric and ready).</li>
 * </ul>
 *
 * <p>Both payloads are scare data pushed <em>to</em> a client, so they are registered S2C.
 * The C2S direction is registered too (same type tokens) to keep the pipeline complete and
 * crash-free if a client ever echoes a payload back.</p>
 */
public final class HorrorNetworking {

    private HorrorNetworking() {}

    /** Call from the common {@link HorrorOperator#onInitialize()}. */
    public static void registerPayloadTypes() {
        // S2C: server authors the scare, client renders it.
        PayloadTypeRegistry.playS2C().register(PanicScarePayload.TYPE, PanicScarePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CountdownPayload.TYPE, CountdownPayload.STREAM_CODEC);

        // C2S: registered for symmetry so the same type can legally travel either way.
        PayloadTypeRegistry.playC2S().register(PanicScarePayload.TYPE, PanicScarePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CountdownPayload.TYPE, CountdownPayload.STREAM_CODEC);

        // Defensive no-op C2S handlers: if a client ever sends one of our payloads we simply
        // ignore it on the main thread rather than letting it reach a missing-handler path.
        ServerPlayNetworking.registerGlobalReceiver(PanicScarePayload.TYPE,
                (payload, context) -> { /* admin-driven only; ignore inbound */ });
        ServerPlayNetworking.registerGlobalReceiver(CountdownPayload.TYPE,
                (payload, context) -> { /* admin-driven only; ignore inbound */ });

        HorrorOperator.LOGGER.info("[HorrorOperator] Registered custom payloads: {}, {}",
                PanicScarePayload.ID, CountdownPayload.ID);
    }

    /** Convenience: send any of our payloads to a specific player from server-side code. */
    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        // canSend guards against a client that somehow lacks the channel (e.g. partial mod set),
        // preventing a server-side crash when piping the high-frequency countdown.
        if (ServerPlayNetworking.canSend(player, payload.type().id())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
