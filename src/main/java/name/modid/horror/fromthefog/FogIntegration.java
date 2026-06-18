package name.modid.horror.fromthefog;

import name.modid.horror.HorrorOperator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Feature 6 — From the Fog (FTF) integration layer.
 *
 * <p>From the Fog uses internal scoreboards to track Herobrine state. We spoof those
 * objectives to trick FTF into triggering its ambient horror effects (footsteps, breathing,
 * shadow figures) without needing a real Herobrine entity spawned.</p>
 *
 * <p>All FTF-specific paths are guarded behind a {@link FabricLoader#isModLoaded(String)}
 * check so this mod is fully functional even when FTF is not installed.</p>
 *
 * <h3>Scoreboard objectives used by FTF</h3>
 * <ul>
 *   <li>{@code ftf_herobrine_pres} — set to 1 on a player to signal presence</li>
 *   <li>{@code ftf_herobrine_dist} — distance in blocks; lower = louder ambient sounds</li>
 * </ul>
 *
 * <p>If FTF is not loaded, the methods here are no-ops that log a debug message.</p>
 */
public final class FogIntegration {

    /** The Fabric mod ID of "From the Fog". */
    private static final String FTF_MOD_ID = "from_the_fog";

    /** Cached result of the FTF presence check (computed once at startup). */
    private static final boolean FTF_LOADED = FabricLoader.getInstance().isModLoaded(FTF_MOD_ID);

    private FogIntegration() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Signals Herobrine's presence to From the Fog for the given target.
     *
     * <p>Sets {@code ftf_herobrine_pres = 1} and {@code ftf_herobrine_dist = spoofedDistance}
     * on the player's scoreboard entries, triggering FTF's ambient audio/visual pipeline.</p>
     *
     * @param target          the player to affect
     * @param server          the running server (used to access the scoreboard)
     * @param spoofedDistance a positive integer representing the spoofed block distance
     *                        (smaller values = more intense ambient effects in FTF)
     */
    public static void applyHerobrinePresence(ServerPlayer target, MinecraftServer server, int spoofedDistance) {
        if (!FTF_LOADED) {
            HorrorOperator.LOGGER.debug("[HorrorOperator] FTF not loaded — skipping presence spoof for {}.", target.getGameProfile().getName());
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        String playerName = target.getGameProfile().getName();

        // Ensure the objectives exist; create them (dummy criterion) if FTF hasn't yet done so
        Objective presenceObj  = getOrCreateObjective(scoreboard, HorrorOperator.FTF_SCOREBOARD_PRESENT);
        Objective distanceObj  = getOrCreateObjective(scoreboard, HorrorOperator.FTF_SCOREBOARD_DISTANCE);

        scoreboard.getOrCreatePlayerScore(target, presenceObj).set(1);
        scoreboard.getOrCreatePlayerScore(target, distanceObj).set(spoofedDistance);

        HorrorOperator.LOGGER.info("[HorrorOperator] FTF presence spoofed for {} (dist={}).", playerName, spoofedDistance);
    }

    /**
     * Clears the Herobrine presence signal from From the Fog for the given target.
     *
     * <p>Resets both scoreboard values to 0, which FTF interprets as Herobrine absent.</p>
     */
    public static void clearHerobrinePresence(ServerPlayer target, MinecraftServer server) {
        if (!FTF_LOADED) return;

        Scoreboard scoreboard = server.getScoreboard();
        String playerName = target.getGameProfile().getName();

        Objective presenceObj  = getOrCreateObjective(scoreboard, HorrorOperator.FTF_SCOREBOARD_PRESENT);
        Objective distanceObj  = getOrCreateObjective(scoreboard, HorrorOperator.FTF_SCOREBOARD_DISTANCE);

        scoreboard.getOrCreatePlayerScore(target, presenceObj).set(0);
        scoreboard.getOrCreatePlayerScore(target, distanceObj).set(0);

        HorrorOperator.LOGGER.info("[HorrorOperator] FTF presence cleared for {}.", playerName);
    }

    /** @return {@code true} if From the Fog is present in the current mod set. */
    public static boolean isFtfLoaded() {
        return FTF_LOADED;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Retrieves an existing scoreboard objective by name, or creates it with a
     * {@link ObjectiveCriteria#DUMMY} criterion if it doesn't yet exist.
     *
     * <p>FTF normally creates these objectives itself on world load. If FTF is installed
     * but the world is fresh, this ensures our writes don't throw a NullPointerException.</p>
     */
    private static Objective getOrCreateObjective(Scoreboard scoreboard, String name) {
        Objective existing = scoreboard.getObjective(name);
        if (existing != null) return existing;

        return scoreboard.addObjective(
                name,
                ObjectiveCriteria.DUMMY,
                net.minecraft.network.chat.Component.literal(name),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
        );
    }
}
