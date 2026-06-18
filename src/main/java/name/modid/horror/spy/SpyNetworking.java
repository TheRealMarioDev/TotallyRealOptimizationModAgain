package name.modid.horror.spy;

import name.modid.horror.HorrorOperator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature 2 — Inventory spy networking controller.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Payload registration ({@link #registerPayloadTypes()}) — both S2C and C2S</li>
 *   <li>Opening a spy session ({@link #openSpyFor(ServerPlayer, ServerPlayer)}) — starts the
 *       periodic sync task and sends the first snapshot immediately</li>
 *   <li>Per-tick sync loop — resends the target's inventory every {@link #SYNC_INTERVAL_TICKS}
 *       ticks so the admin sees live changes</li>
 *   <li>Handling inbound {@link SpyActionPayload} — applies slot mutations to the target
 *       inventory silently (no container animation, no sound)</li>
 * </ul>
 */
public final class SpyNetworking {

    /** How many server ticks between inventory snapshots sent to the admin. */
    private static final int SYNC_INTERVAL_TICKS = 5;

    /** Maps admin UUID → target UUID for active spy sessions. */
    private static final Map<UUID, UUID> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private static int tickCounter = 0;
    private static boolean registered = false;

    private SpyNetworking() {}

    // -----------------------------------------------------------------------
    // Initialisation
    // -----------------------------------------------------------------------

    /** Register payload types. Call from the common initializer. */
    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(SpyPayload.TYPE, SpyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SpyActionPayload.TYPE, SpyActionPayload.STREAM_CODEC);

        // Server-side handler for admin actions coming back from the spy screen
        ServerPlayNetworking.registerGlobalReceiver(SpyActionPayload.TYPE, SpyNetworking::handleSpyAction);

        HorrorOperator.LOGGER.info("[HorrorOperator] SpyNetworking payloads registered.");
    }

    /** Register the tick loop. Call from the common initializer after payload types. */
    public static void registerTickLoop() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(SpyNetworking::tick);
    }

    // -----------------------------------------------------------------------
    // Session management
    // -----------------------------------------------------------------------

    /**
     * Opens a spy session: immediately sends the first snapshot to {@code admin} and begins
     * the repeating sync task.
     *
     * @param admin  the operator requesting the spy view
     * @param target the player whose inventory will be mirrored
     */
    public static void openSpyFor(ServerPlayer admin, ServerPlayer target) {
        ACTIVE_SESSIONS.put(admin.getUUID(), target.getUUID());
        sendSnapshot(admin, target);
        HorrorOperator.LOGGER.info("[HorrorOperator] Spy session opened: {} → {}.",
                admin.getGameProfile().getName(), target.getGameProfile().getName());
    }

    /**
     * Closes any active spy session for the given admin and stops syncing.
     */
    public static void closeSpyFor(ServerPlayer admin) {
        if (ACTIVE_SESSIONS.remove(admin.getUUID()) != null) {
            HorrorOperator.LOGGER.info("[HorrorOperator] Spy session closed for {}.", admin.getGameProfile().getName());
        }
    }

    // -----------------------------------------------------------------------
    // Tick loop
    // -----------------------------------------------------------------------

    private static void tick(MinecraftServer server) {
        if (ACTIVE_SESSIONS.isEmpty()) return;
        tickCounter++;
        if (tickCounter % SYNC_INTERVAL_TICKS != 0) return;

        ACTIVE_SESSIONS.entrySet().removeIf(entry -> {
            ServerPlayer admin  = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getValue());

            if (admin == null || target == null) return true; // Remove stale session

            sendSnapshot(admin, target);
            return false;
        });
    }

    // -----------------------------------------------------------------------
    // Snapshot builder
    // -----------------------------------------------------------------------

    private static void sendSnapshot(ServerPlayer admin, ServerPlayer target) {
        List<ItemStack> slots = new ArrayList<>();

        // Main inventory (36 slots: hotbar 0–8, then rows 1–3)
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            slots.add(target.getInventory().getItem(i).copy());
        }
        // Armor slots (4) — index 36–39 in the vanilla inventory ordering
        for (int i = 0; i < 4; i++) {
            slots.add(target.getInventory().armor.get(i).copy());
        }
        // Offhand slot (1)
        slots.add(target.getInventory().offhand.get(0).copy());

        SpyPayload payload = new SpyPayload(slots, target.getGameProfile().getName());
        if (ServerPlayNetworking.canSend(admin, SpyPayload.ID)) {
            ServerPlayNetworking.send(admin, payload);
        }
    }

    // -----------------------------------------------------------------------
    // Inbound action handler
    // -----------------------------------------------------------------------

    private static void handleSpyAction(SpyActionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer admin = context.player();
        MinecraftServer server = admin.getServer();
        if (server == null) return;

        // Verify this admin actually has a spy session open for the claimed target
        UUID targetId = ACTIVE_SESSIONS.get(admin.getUUID());
        if (targetId == null) return;

        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target == null) return;

        // Validate that the claimed target name matches the session target (prevents spoofing)
        if (!target.getGameProfile().getName().equals(payload.targetName())) return;

        context.server().execute(() -> {
            switch (payload.action()) {
                case SpyActionPayload.ACTION_SET -> {
                    int slot = payload.slotIndex();
                    if (slot >= 0 && slot < target.getInventory().getContainerSize()) {
                        target.getInventory().setItem(slot, payload.stack().copy());
                    }
                }
                case SpyActionPayload.ACTION_DELETE -> {
                    int slot = payload.slotIndex();
                    if (slot >= 0 && slot < target.getInventory().getContainerSize()) {
                        target.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                }
                case SpyActionPayload.ACTION_CLOSE -> closeSpyFor(admin);
            }
        });
    }
}
