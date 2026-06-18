package name.modid.horror;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central constants hub for the HorrorOperator system.
 *
 * <p>All subsystem classes reference {@link #MOD_ID} and {@link #LOGGER} from here
 * rather than the outer {@code OptimizationMod} class to keep the server-side horror
 * packages self-contained and independent of the entrypoint class.</p>
 *
 * <p>From the outside, this mod appears as a mundane "OptimizationMod". The HorrorOperator
 * identity only surfaces in internal logs and NBT tags — never in player-facing strings.</p>
 */
public final class HorrorOperator {

    /** Must match the {@code id} field in {@code fabric.mod.json}. */
    public static final String MOD_ID = "optimizationmod";

    /** Internal logger. Uses the mod ID so all horror log lines are clearly attributed. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "/horror");

    // -----------------------------------------------------------------------
    // NBT / Scoreboard Keys (shared between server and client data-driver code)
    // -----------------------------------------------------------------------

    /** Persistent player data compound tag that stores the Herobrine curse flag. */
    public static final String NBT_CURSE_KEY = "HorrorOperator.cursed";

    /**
     * From the Fog scoreboard objective that signals Herobrine's presence.
     * Set to 1 to trigger ambient FTF behaviour; 0 to clear it.
     */
    public static final String FTF_SCOREBOARD_PRESENT  = "ftf_herobrine_pres";

    /**
     * From the Fog scoreboard objective that carries the spoofed Herobrine distance.
     * FTF reads this to scale ambient sound volume.
     */
    public static final String FTF_SCOREBOARD_DISTANCE = "ftf_herobrine_dist";

    private HorrorOperator() {}
}
