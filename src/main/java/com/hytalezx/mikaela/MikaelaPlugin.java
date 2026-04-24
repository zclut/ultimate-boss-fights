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
        BossRegistry.register(new BossConfig("Mikaela", "MIKAELA WARDERER",75.0, "mikaela.ui"));
        BossRegistry.register(new BossConfig("Arcangel", "MIKAELA ARCHANGEL",75.0, "arcangel.ui"));


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
        NpcDeathRespawnSystem.register(
                "Mikaela", "Arcangel",
                7.0f,               // segundos hasta phase 2
                "Mikaela_Death_Legendary",
                10.0f,              // scale
                7.0f                // duración del particle — ajustar aquí
        );
    }
}