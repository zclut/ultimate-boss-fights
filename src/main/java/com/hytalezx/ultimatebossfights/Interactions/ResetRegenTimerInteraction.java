package com.hytalezx.ultimatebossfights.Interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.time.Instant;

/**
 * Resets the NPC's NoDamageTaken regen timer by updating DamageDataComponent.lastDamageTime
 * to now. This prevents passive health regen from kicking in after long attacks.
 *
 * JSON usage:
 * { "Type": "HytaleZX:ResetRegenTimer" }
 */
public class ResetRegenTimerInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<ResetRegenTimerInteraction> CODEC =
            BuilderCodec.builder(ResetRegenTimerInteraction.class, ResetRegenTimerInteraction::new, SimpleInstantInteraction.CODEC)
                    .build();

    public ResetRegenTimerInteraction() {}

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();
        Ref<EntityStore> npcRef = context.getOwningEntity();

        if (npcRef == null || !npcRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        DamageDataComponent damageData = buffer.getComponent(npcRef, DamageDataComponent.getComponentType());
        if (damageData == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        damageData.setLastDamageTime(Instant.now());
        LOGGER.atInfo().log("[ResetRegenTimer] reset NoDamageTaken timer for NPC");

        context.getState().state = InteractionState.Finished;
    }
}
