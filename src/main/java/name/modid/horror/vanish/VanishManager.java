package name.modid.horror.vanish;

import name.modid.horror.HorrorOperator;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature 2 — Advanced vanish state.
 *
 * <p>Holds the set of currently-vanished operator UUIDs. The actual <em>enforcement</em> of
 * invisibility happens in three mixins that all consult this manager:</p>
 * <ul>
 *     <li>{@code TrackedEntityVanishMixin} — drops vanished players from every non-admin
 *         client's entity tracker (no spawn packet, no movement, no entity-attached sounds,
 *         no particles).</li>
 *     <li>{@code ServerLevelVanishMixin} — cancels {@code levelEvent} broadcasts (block break
 *         particles + sound) caused by a vanished player.</li>
 *     <li>{@code ContainerOpenersCounterMixin} — cancels chest/barrel/shulker open &amp; close
 *         sounds and lid animation when the opener is vanished.</li>
 * </ul>
 *
 * <p>State is intentionally session-only (a vanish does not survive a server restart) — this
 * is an operator tool, not persistent player data. The set is concurrent because the entity
 * tracker mixin can be touched from the server thread while commands mutate it.</p>
 */
public final class VanishManager {

    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

    private VanishManager() {}

    public static boolean isVanished(UUID id) {
        return id != null && VANISHED.contains(id);
    }

    public static boolean isVanished(ServerPlayer player) {
        return player != null && VANISHED.contains(player.getUUID());
    }

    /** @return the new vanished state after toggling. */
    public static boolean toggle(ServerPlayer admin) {
        if (isVanished(admin)) {
            setVanished(admin, false);
            return false;
        } else {
            setVanished(admin, true);
            return true;
        }
    }

    public static void setVanished(ServerPlayer admin, boolean vanished) {
        UUID id = admin.getUUID();
        if (vanished) {
            VANISHED.add(id);
            hideFromEveryone(admin);
            HorrorOperator.LOGGER.info("[HorrorOperator] {} vanished.", admin.getGameProfile().getName());
        } else {
            VANISHED.remove(id);
            revealToEveryone(admin);
            HorrorOperator.LOGGER.info("[HorrorOperator] {} revealed.", admin.getGameProfile().getName());
        }
    }

    /**
     * Push the disappearance immediately rather than waiting for the next tracker tick:
     * remove the entity client-side and strip the player from the tab list for everyone else.
     * The {@code TrackedEntityVanishMixin} then keeps them gone on subsequent ticks.
     */
    private static void hideFromEveryone(ServerPlayer admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(admin.getId());
        ClientboundPlayerInfoRemovePacket removeTab =
                new ClientboundPlayerInfoRemovePacket(List.of(admin.getUUID()));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == admin) continue;
            other.connection.send(removeEntity);
            other.connection.send(removeTab);
        }
    }

    /** Re-add to tab list; the tracker mixin will re-spawn the entity on the next update tick. */
    private static void revealToEveryone(ServerPlayer admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        ClientboundPlayerInfoUpdatePacket addTab =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singletonList(admin));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == admin) continue;
            other.connection.send(addTab);
        }
    }
}
