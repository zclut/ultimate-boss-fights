package com.hytalezx.mikaela.Systems;

import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytalezx.mikaela.Components.GrabbedComponent;
import com.hytalezx.mikaela.Interactions.GrabInteraction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class GrabbedTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, GrabbedComponent> grabbedType;

    public GrabbedTickSystem(ComponentType<EntityStore, GrabbedComponent> grabbedType) {
        this.grabbedType = grabbedType;
    }

    @NullableDecl
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @SuppressWarnings("unchecked")
    public void tick(float delta, int idx,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        GrabbedComponent grabbed = chunk.getComponent(idx, grabbedType);
        if (grabbed == null) return;

        LOGGER.atInfo().log("[GrabbedTickSystem] procesando grab — ticksRemaining=%d", grabbed.ticksRemaining);

        Ref<EntityStore> playerRef = chunk.getReferenceTo(idx);
        Player player = chunk.getComponent(idx, Player.getComponentType());
        if (player == null) {
            releasePlayer(playerRef, store, commandBuffer, grabbed);
            return;
        }

        // ── Verificar que el NPC sigue vivo ──────────────────────────────────
        boolean npcAlive = false;
        if (grabbed.grabberRef != null && grabbed.grabberRef.isValid()) {
            EntityStatMap statMap = store.getComponent(
                    (Ref<EntityStore>) grabbed.grabberRef, EntityStatMap.getComponentType());
            if (statMap != null) {
                var hp = statMap.get(DefaultEntityStatTypes.getHealth());
                npcAlive = hp != null && hp.asPercentage() > 0.0;
            }
        }

        if (!npcAlive) {
            releasePlayer(playerRef, store, commandBuffer, grabbed);
            return;
        }

        // ── Restaurar rotación original via HeadRotation cada tick ───────────
        HeadRotation headRotation = chunk.getComponent(idx, HeadRotation.getComponentType());
        if (headRotation != null) {
            headRotation.teleportRotation(
                    new Vector3f(grabbed.originalRotationX, grabbed.originalRotationY, 0)
            );
        }
        TransformComponent playerTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
        if (playerTransform != null) {
            Vector3f rot = playerTransform.getRotation();
            playerTransform.setRotation(
                    new Vector3f(grabbed.originalRotationX, grabbed.originalRotationY, rot.z)
            );
        }

        // ── Inmovilizar al jugador cada tick ──────────────────────────────────
        MovementStatesComponent movComp = chunk.getComponent(idx, MovementStatesComponent.getComponentType());
        if (movComp != null) {
            MovementStates states = movComp.getMovementStates();
            states.mounting        = true;
            states.forcedCrouching = true;
            states.idle            = true;
            states.horizontalIdle  = true;
        }

        // ── Fuerza de atracción hacia el punto de anclaje del NPC ─────────────
        if (grabbed.grabberRef != null && grabbed.grabberRef.isValid()) {
            TransformComponent npcTransform = store.getComponent(
                    (Ref<EntityStore>) grabbed.grabberRef, TransformComponent.getComponentType());
            Velocity vel = chunk.getComponent(idx, Velocity.getComponentType());

            if (npcTransform != null && playerTransform != null && vel != null) {
                Vector3d npcPos    = npcTransform.getPosition();
                Vector3d playerPos = playerTransform.getPosition();

                double dx = (npcPos.x + grabbed.anchorOffsetX) - playerPos.x;
                double dy = (npcPos.y + grabbed.anchorOffsetY) - playerPos.y;
                double dz = (npcPos.z + grabbed.anchorOffsetZ) - playerPos.z;

                vel.setZero();
                vel.addForce(new Vector3d(dx * 0.8, dy * 0.8, dz * 0.8));
            }
        }

        // ── Montar al jugador en el NPC en el primer tick ─────────────────────
        NPCMountComponent existingMount = store.getComponent(
                (Ref<EntityStore>) grabbed.grabberRef, NPCMountComponent.getComponentType());

        if (existingMount == null) {
            PlayerRef playerRefComponent = chunk.getComponent(idx, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                NPCMountComponent mountComponent = new NPCMountComponent();
                NPCEntity npc = store.getComponent(
                        (Ref<EntityStore>) grabbed.grabberRef, NPCEntity.getComponentType());
                if (npc != null) {
                    mountComponent.setOriginalRoleIndex(npc.getRoleIndex());
                }
                mountComponent.setOwnerPlayerRef(playerRefComponent);
                mountComponent.setAnchor(
                        grabbed.anchorOffsetX,
                        grabbed.anchorOffsetY,
                        grabbed.anchorOffsetZ
                );
                commandBuffer.addComponent(
                        (Ref<EntityStore>) grabbed.grabberRef,
                        NPCMountComponent.getComponentType(),
                        mountComponent
                );

                MountedComponent mountedComp = new MountedComponent(
                        (Ref<EntityStore>) grabbed.grabberRef,
                        new Vector3f(grabbed.anchorOffsetX, grabbed.anchorOffsetY, grabbed.anchorOffsetZ),
                        MountController.Minecart
                );
                commandBuffer.addComponent(playerRef, MountedComponent.getComponentType(), mountedComp);

                LOGGER.atInfo().log("[GrabbedTickSystem] jugador montado en NPC");
            }
        }

        // ── Daño por tick ─────────────────────────────────────────────────────
        grabbed.damageTickCounter++;
        if (grabbed.damageTickCounter >= grabbed.damageIntervalTicks) {
            grabbed.damageTickCounter = 0;
            EntityStatMap statMap = chunk.getComponent(idx, EntityStatMap.getComponentType());
            if (statMap != null) {
                statMap.subtractStatValue(DefaultEntityStatTypes.getHealth(), grabbed.damagePerTick);
            }
        }

        // ── Countdown y release ───────────────────────────────────────────────
        grabbed.ticksRemaining--;
        if (grabbed.ticksRemaining <= 0) {
            releasePlayer(playerRef, store, commandBuffer, grabbed);
        }
    }

    private void releasePlayer(Ref<EntityStore> playerRef,
                               Store<EntityStore> store,
                               CommandBuffer<EntityStore> commandBuffer,
                               GrabbedComponent grabbed) {

        if (grabbed.grabberRef != null && grabbed.grabberRef.isValid()) {
            try {
                NPCMountComponent mount = store.getComponent(
                        (Ref<EntityStore>) grabbed.grabberRef, NPCMountComponent.getComponentType());
                if (mount != null) {
                    commandBuffer.removeComponent(
                            (Ref<EntityStore>) grabbed.grabberRef,
                            NPCMountComponent.getComponentType()
                    );
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("[GrabbedTickSystem] error al desmontar NPC: %s", e.getMessage());
            }
        }

        try {
            if (store.getComponent(playerRef, MountedComponent.getComponentType()) != null) {
                commandBuffer.removeComponent(playerRef, MountedComponent.getComponentType());
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[GrabbedTickSystem] error al quitar MountedComponent: %s", e.getMessage());
        }

        MovementStatesComponent movComp = store.getComponent(
                playerRef, MovementStatesComponent.getComponentType());
        if (movComp != null) {
            MovementStates states = movComp.getMovementStates();
            states.forcedCrouching = false;
            states.idle            = false;
            states.mounting        = false;
            states.horizontalIdle  = false;
        }

        GrabInteraction.PENDING_GRABS.remove(playerRef);
        try {
            if (store.getComponent(playerRef, grabbedType) != null) {
                commandBuffer.removeComponent(playerRef, grabbedType);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[GrabbedTickSystem] error al quitar GrabbedComponent: %s", e.getMessage());
        }
    }
}