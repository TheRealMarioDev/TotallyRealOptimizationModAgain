package name.modid;

import name.modid.horror.HorrorOperator;
import name.modid.horror.command.HorrorCommands;
import name.modid.horror.countdown.CountdownEngine;
import name.modid.horror.curse.CurseRespawnHandler;
import name.modid.horror.network.HorrorNetworking;
import name.modid.horror.scare.AmbientScareManager;
import name.modid.horror.spy.SpyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import name.modid.horror.config.HorrorConfigManager;

/**
 * Common (server + client) mod entry point.
 *
 * <p>Poses publicly as a mundane "OptimizationMod" — the class name, mod ID, and
 * fabric.mod.json description are all deliberately innocuous. All horror logic is
 * wired in here but never exposed to players via the mod menu or server list.</p>
 *
 * <p>Initialisation order matters: payloads must be registered before commands, and
 * event hooks must be registered before the first tick.</p>
 */
public class OptimizationMod implements ModInitializer {

    /** Public mod ID — matches fabric.mod.json and all ResourceLocation namespaces. */
    public static final String MOD_ID = HorrorOperator.MOD_ID;

    @Override
    public void onInitialize() {
        HorrorOperator.LOGGER.info("[HorrorOperator] Initialising subsystems...");
        HorrorConfigManager.load();
        // 1. Register all custom payload types with PayloadTypeRegistry (both S2C and C2S)
        //    Must happen before any command or event can try to send a packet.
        HorrorNetworking.registerPayloadTypes();
        SpyNetworking.registerPayloadTypes();


        // 2. Register Fabric event hooks that must be active before the first world tick
        CurseRespawnHandler.register();   // ServerPlayerEvents.COPY_FROM — keeps curse alive through death
        CountdownEngine.register();       // ServerTickEvents.END_SERVER_TICK — countdown loop
        AmbientScareManager.register();   // ServerTickEvents.END_SERVER_TICK — timed ambient scares
        SpyNetworking.registerTickLoop(); // ServerTickEvents.END_SERVER_TICK — inventory sync loop

        // 3. Register all Brigadier commands (/vanish, /scare, /curse, /spy)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                HorrorCommands.register(dispatcher, registryAccess, environment)
        );

        HorrorOperator.LOGGER.info("[HorrorOperator] All subsystems online.");
    }
}
