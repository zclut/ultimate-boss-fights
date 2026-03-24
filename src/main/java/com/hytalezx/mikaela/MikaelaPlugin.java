package com.hytalezx.mikaela;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalezx.mikaela.Command.MikaelaCommand;
import com.hytalezx.mikaela.Components.GrabbedComponent;
import com.hytalezx.mikaela.Config.BossConfig;
import com.hytalezx.mikaela.Config.BossRegistry;
import com.hytalezx.mikaela.Interactions.GrabInteraction;
import com.hytalezx.mikaela.Systems.BossTickingSystem;
import com.hytalezx.mikaela.Systems.GrabbedTickSystem;
import com.hytalezx.mikaela.Systems.NpcDeathRespawnSystem;

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

        // BOSS REGISTRY
        LOGGER.atInfo().log("Registering bosses...");
        BossRegistry.register(new BossConfig("Mikaela", "Mikaela",50.0));

        // ── COMPONENTS ──────────────────────────────────────────────────────
//        LOGGER.atInfo().log("Registering components...");
//        ComponentType<EntityStore, GrabbedComponent> grabbedType =
//                this.getEntityStoreRegistry().registerComponent(
//                        GrabbedComponent.class,
//                        GrabbedComponent::new
//                );

        // ── INTERACTIONS ────────────────────────────────────────────────────
//        LOGGER.atInfo().log("Registering interactions...");
//        GrabInteraction.grabbedType = grabbedType;
//        getCodecRegistry(Interaction.CODEC).register(
//                "HytaleZX:Grab",
//                GrabInteraction.class,
//                GrabInteraction.CODEC
//        );

        // SYSTEMS
        LOGGER.atInfo().log("Registering systems...");
        this.getEntityStoreRegistry().registerSystem(new BossTickingSystem());
//        this.getEntityStoreRegistry().registerSystem(new GrabbedTickSystem(grabbedType));

        // NEW PHASE SYSTEM
        EntityStore.REGISTRY.registerSystem(new NpcDeathRespawnSystem());
        NpcDeathRespawnSystem.register("Mikaela", "MikaelaPhase", 5.0f);
    }
}