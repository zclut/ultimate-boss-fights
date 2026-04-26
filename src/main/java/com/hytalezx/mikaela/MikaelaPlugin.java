package com.hytalezx.mikaela;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalezx.mikaela.Command.MikaelaCommand;
import com.hytalezx.mikaela.Config.BossConfig;
import com.hytalezx.mikaela.Config.BossRegistry;

import com.hytalezx.mikaela.Interactions.FallingProjectileInteraction;
import com.hytalezx.mikaela.Interactions.ResetRegenTimerInteraction;
import com.hytalezx.mikaela.Systems.BossTickingSystem;
import com.hytalezx.mikaela.Systems.DeathParticleTickSystem;
import com.hytalezx.mikaela.Systems.FallingProjectileTickSystem;
import com.hytalezx.mikaela.Systems.NpcDeathRespawnSystem;
import com.hytalezx.mikaela.Systems.PlayerBossHudSystem;

import javax.annotation.Nonnull;


public class MikaelaPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final BossConfig[] BOSS_CONFIGS = {
        new BossConfig("Mikaela", "MIKAELA WARDERER",  55.0, "Pages/HUD/Mikaela/mikaela.ui"),
        new BossConfig("Arcangel", "MIKAELA ARCHANGEL", 55.0, "Pages/HUD/Arcangel/arcangel.ui"),
    };

    private record PhaseTransition(String from, String to, float delay,
                                   String particle, float scale, float duration) {}

    private static final PhaseTransition[] PHASE_TRANSITIONS = {
        new PhaseTransition("Mikaela", "Arcangel", 7.0f, "Mikaela_Death_Legendary", 10.0f, 7.0f),
    };

    public MikaelaPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        super.setup();

        // COMMAND REGISTRY
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new MikaelaCommand(this.getName(), this.getManifest().getVersion().toString()));


        // BOSS REGISTRY HEALTH BARS
        LOGGER.atInfo().log("Registering bosses...");
        for (BossConfig cfg : BOSS_CONFIGS) BossRegistry.register(cfg);


        // ── INTERACTIONS ────────────────────────────────────────────────────
        // ── FallingProjectile (FallingProjectile interaction) ──────────────────────────
        LOGGER.atInfo().log("Registering FallingProjectile interaction...");
        getCodecRegistry(Interaction.CODEC).register(
                "HytaleZX:FallingProjectile",
                FallingProjectileInteraction.class,
                FallingProjectileInteraction.CODEC
        );


        // ── ResetRegenTimer ────────────────────────────────────────────────
        LOGGER.atInfo().log("Registering ResetRegenTimer interaction...");
        getCodecRegistry(Interaction.CODEC).register(
                "HytaleZX:ResetRegenTimer",
                ResetRegenTimerInteraction.class,
                ResetRegenTimerInteraction.CODEC
        );


        // ── SYSTEMS ────────────────────────────────────────────────────────
        LOGGER.atInfo().log("Registering systems...");
        this.getEntityStoreRegistry().registerSystem(new BossTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new PlayerBossHudSystem());
        this.getEntityStoreRegistry().registerSystem(new FallingProjectileTickSystem());
        this.getEntityStoreRegistry().registerSystem(new DeathParticleTickSystem());


        // ── NEW PHASE SYSTEM ────────────────────────────────────────────────
        EntityStore.REGISTRY.registerSystem(new NpcDeathRespawnSystem());
        for (PhaseTransition t : PHASE_TRANSITIONS)
            NpcDeathRespawnSystem.register(t.from(), t.to(), t.delay(), t.particle(), t.scale(), t.duration());
    }
}