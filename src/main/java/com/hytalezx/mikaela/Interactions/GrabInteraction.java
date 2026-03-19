package com.hytalezx.mikaela.Interactions;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalezx.mikaela.Components.GrabbedComponent;
import javax.annotation.Nonnull;

public class GrabInteraction extends SimpleInstantInteraction {

    private float duration    = 3.0f;  // segundos
    private float damage      = 10.0f; // daño total repartido durante la grab
    private float anchorOffsetX  = 0.0f;
    private float anchorOffsetY  = 1.0f;
    private float anchorOffsetZ  = 1.5f;

    public static final BuilderCodec<GrabInteraction> CODEC =
            BuilderCodec.builder(GrabInteraction.class, GrabInteraction::new, SimpleInstantInteraction.CODEC)
                    .append(new KeyedCodec<>("Duration", Codec.FLOAT),
                            (g, v) -> g.duration = v, g -> g.duration).add()
                    .append(new KeyedCodec<>("Damage",   Codec.FLOAT),
                            (g, v) -> g.damage   = v, g -> g.damage).add()
                    .append(new KeyedCodec<>("AnchorOffsetX", Codec.FLOAT),
                            (g, v) -> g.anchorOffsetX = v, g -> g.anchorOffsetX).add()
                    .append(new KeyedCodec<>("AnchorOffsetY", Codec.FLOAT),
                            (g, v) -> g.anchorOffsetY = v, g -> g.anchorOffsetY).add()
                    .append(new KeyedCodec<>("AnchorOffsetZ", Codec.FLOAT),
                            (g, v) -> g.anchorOffsetZ = v, g -> g.anchorOffsetZ).add()
                    .build();

    public static ComponentType<EntityStore, GrabbedComponent> grabbedType;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Refs de jugadores con un grab YA ENCOLADO en el CommandBuffer pero
     * aún no aplicado al store. Evita el addComponent duplicado dentro del
     * mismo tick. Se limpia cuando el GrabbedTickSystem elimina el componente.
     */
    public static final Set<Ref<EntityStore>> PENDING_GRABS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public GrabInteraction() {}

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        // En HitEntity: context.getEntity() = jugador golpeado, buffer.getComponent() para acceder al store
        LOGGER.atInfo().log("[GrabInteraction] firstRun() disparado");

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();

        // getEntity()       = runningForEntity = el NPC que ejecuta la cadena
        // getOwningEntity() = owningEntity     = también el NPC
        // getTargetEntity() = TARGET_ENTITY    = entidad golpeada por el Selector ✅
        Ref<EntityStore> targetRef = context.getTargetEntity();

        LOGGER.atInfo().log("[GrabInteraction] targetEntity=%s", targetRef != null);

        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.atWarning().log("[GrabInteraction] targetEntity null — Selector no alcanzó a nadie");
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = buffer.getComponent(targetRef, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("[GrabInteraction] targetEntity no es Player");
            context.getState().state = InteractionState.Failed;
            return;
        }

        LOGGER.atInfo().log("[GrabInteraction] jugador encontrado, aplicando grab...");

        var store = buffer.getExternalData().getStore();

        // Guard doble: store (aplicado) + PENDING_GRABS (encolado aún no aplicado)
        if (store.getComponent(targetRef, grabbedType) != null
                || PENDING_GRABS.contains(targetRef)) {
            LOGGER.atInfo().log("[GrabInteraction] jugador ya está grabbed o grab pendiente — skip");
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Marcar como pendiente ANTES de encolar el addComponent
        PENDING_GRABS.add(targetRef);

        LOGGER.atInfo().log("[GrabInteraction] aplicando GrabbedComponent al jugador");

        // ✅ npcRef no está disponible en HitEntity — lo dejamos null.
        // GrabbedTickSystem usará la posición del jugador como anchor fijo.
        // getOwningEntity() = el NPC que disparó la cadena
        Ref<EntityStore> npcRef = context.getOwningEntity();

        // Convertir segundos a ticks (20 ticks/s) y repartir el daño por tick
        int   durationTicks  = Math.max(1, Math.round(duration * 20));
        float damagePerTick  = damage / durationTicks;

        GrabbedComponent grabbedComp = new GrabbedComponent(
                npcRef, durationTicks, damagePerTick, 1, // damageInterval=1 → cada tick
                anchorOffsetX, anchorOffsetY, anchorOffsetZ
        );

        // Guardar rotación original del jugador para no girarle la cámara
        TransformComponent playerTransform = buffer.getComponent(
                targetRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
        if (playerTransform != null) {
            grabbedComp.originalRotationX = playerTransform.getRotation().x;
            grabbedComp.originalRotationY = playerTransform.getRotation().y;
        }

        buffer.addComponent(targetRef, grabbedType, grabbedComp);

        MovementStatesComponent movComp = buffer.getComponent(
                targetRef, MovementStatesComponent.getComponentType());
        if (movComp != null) {
            MovementStates states = movComp.getMovementStates();
            states.forcedCrouching = true;
            states.idle            = true;
            states.horizontalIdle  = true;
        }

        Velocity vel = buffer.getComponent(targetRef, Velocity.getComponentType());
        if (vel != null) vel.setZero();

        context.getState().state = InteractionState.Finished;
    }
}