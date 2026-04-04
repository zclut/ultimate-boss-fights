package com.hytalezx.mikaela.Interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalezx.mikaela.Components.HitboxOffsetComponent;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApplyHitboxInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private float offsetY = 0f;
    private float speed = 0.5f;
    private float duration = 2.0f;

    public static ComponentType<EntityStore, HitboxOffsetComponent> hitboxOffsetType;

    public static final Set<Ref<EntityStore>> PENDING_OFFSETS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static final BuilderCodec<ApplyHitboxInteraction> CODEC =
            BuilderCodec.builder(ApplyHitboxInteraction.class, ApplyHitboxInteraction::new, SimpleInstantInteraction.CODEC)
                    .append(new KeyedCodec<>("OffsetY", Codec.FLOAT),
                            (i, v) -> i.offsetY = v, i -> i.offsetY).add()
                    .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                            (i, v) -> i.speed = v, i -> i.speed).add()
                    .append(new KeyedCodec<>("Duration", Codec.FLOAT),
                            (i, v) -> i.duration = v, i -> i.duration).add()
                    .build();

    public ApplyHitboxInteraction() {}

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            LOGGER.atWarning().log("[ApplyHitbox] entity null");
            context.getState().state = InteractionState.Failed;
            return;
        }

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();

        var store = buffer.getExternalData().getStore();
        if (store.getComponent(entityRef, hitboxOffsetType) != null
                || PENDING_OFFSETS.contains(entityRef)) {
            LOGGER.atInfo().log("[ApplyHitbox] ya tiene HitboxOffsetComponent o pendiente — skip");
            context.getState().state = InteractionState.Finished;
            return;
        }

        PENDING_OFFSETS.add(entityRef);

        int durationTicks = Math.max(1, Math.round(duration * 20));
        HitboxOffsetComponent comp = new HitboxOffsetComponent(offsetY, speed, durationTicks);
        buffer.addComponent(entityRef, hitboxOffsetType, comp);

        LOGGER.atInfo().log("[ApplyHitbox] aplicado — offsetY=%.1f speed=%.2f duration=%.1fs ticks=%d",
                offsetY, speed, duration, durationTicks);

        context.getState().state = InteractionState.Finished;
    }
}
