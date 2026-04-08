package com.hytalezx.mikaela.Interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Custom interaction that spawns a projectile from the sky above the target entity.
 * Used inside a Selector's HitEntity chain.
 *
 * Velocity, Gravity, EntityDamageRadius etc. are configured in the Projectile JSON config.
 *
 * JSON usage:
 * {
 *   "Type": "HytaleZX:FallingProjectile",
 *   "ProjectileId": "Mikaela_Sky_Projectile",
 *   "Height": 20.0,
 *   "OffsetX": 0.0,
 *   "OffsetY": 0.0,
 *   "OffsetZ": 0.0,
 *   "WarningParticle": "Fire_AoE_Spawn",
 *   "WarningParticleScale": 5.0
 * }
 */
public class FallingProjectile extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private String projectileId = "Mikaela_Sky_Projectile";
    private float height = 20.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float offsetZ = 0f;
    private String warningParticle = "";
    private float warningParticleScale = 1.0f;

    public static final BuilderCodec<FallingProjectile> CODEC =
            BuilderCodec.builder(FallingProjectile.class, FallingProjectile::new, SimpleInstantInteraction.CODEC)
                    .append(new KeyedCodec<>("ProjectileId", Codec.STRING),
                            (i, v) -> i.projectileId = v, i -> i.projectileId).add()
                    .append(new KeyedCodec<>("Height", Codec.FLOAT),
                            (i, v) -> i.height = v, i -> i.height).add()
                    .append(new KeyedCodec<>("OffsetX", Codec.FLOAT),
                            (i, v) -> i.offsetX = v, i -> i.offsetX).add()
                    .append(new KeyedCodec<>("OffsetY", Codec.FLOAT),
                            (i, v) -> i.offsetY = v, i -> i.offsetY).add()
                    .append(new KeyedCodec<>("OffsetZ", Codec.FLOAT),
                            (i, v) -> i.offsetZ = v, i -> i.offsetZ).add()
                    .append(new KeyedCodec<>("WarningParticle", Codec.STRING),
                            (i, v) -> i.warningParticle = v, i -> i.warningParticle).add()
                    .append(new KeyedCodec<>("WarningParticleScale", Codec.FLOAT),
                            (i, v) -> i.warningParticleScale = v, i -> i.warningParticleScale).add()
                    .build();

    public FallingProjectile() {}

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();

        Ref<EntityStore> targetRef = context.getTargetEntity();
        Ref<EntityStore> npcRef = context.getOwningEntity();

        if (targetRef == null || !targetRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Skip non-player targets (Selector can hit the NPC itself)
        Player player = buffer.getComponent(targetRef, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        TransformComponent targetTransform = buffer.getComponent(
                targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUIDComponent uuidComp = buffer.getComponent(npcRef, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID shooterUuid = uuidComp.getUuid();

        Vector3d targetPos = targetTransform.getPosition();

        // Landing position = target + offset
        double landX = targetPos.x + offsetX;
        double landY = targetPos.y + offsetY;
        double landZ = targetPos.z + offsetZ;

        // Spawn warning particle at landing position
        if (warningParticle != null && !warningParticle.isEmpty()) {
            try {
                ParticleUtil.spawnParticleEffect(
                        warningParticle,
                        new Vector3d(landX, landY, landZ),
                        buffer);
            } catch (Exception e) {
                LOGGER.atWarning().log("[FallingProjectile] warning particle failed: %s", e.getMessage());
            }
        }

        // Spawn position: directly above the landing point
        Vector3d spawnPos = new Vector3d(landX, landY + height, landZ);
        Vector3f spawnRot = new Vector3f(0, -90, 0);

        TimeResource timeResource = buffer.getResource(TimeResource.getResourceType());

        Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(
                timeResource, projectileId, spawnPos, spawnRot);

        ProjectileComponent projComp = holder.getComponent(ProjectileComponent.getComponentType());
        if (projComp == null) {
            LOGGER.atWarning().log("[FallingProjectile] failed to assemble projectile '%s'", projectileId);
            context.getState().state = InteractionState.Failed;
            return;
        }

        holder.ensureComponent(Intangible.getComponentType());

        if (projComp.getProjectile() == null) {
            projComp.initialize();
            if (projComp.getProjectile() == null) {
                LOGGER.atWarning().log("[FallingProjectile] projectile config '%s' not found", projectileId);
                context.getState().state = InteractionState.Failed;
                return;
            }
        }

        // shoot() sets creatorUuid and velocity from projectile config
        projComp.shoot(holder, shooterUuid,
                spawnPos.x, spawnPos.y, spawnPos.z,
                0f, -90f);

        // Override position to exact spawn point (undo computeStartOffset)
        TransformComponent projTransform = holder.getComponent(TransformComponent.getComponentType());
        if (projTransform != null) {
            projTransform.setPosition(new Vector3d(landX, landY + height, landZ));
        }

        // Override velocity: perfectly straight down using MuzzleVelocity from config
        double speed = projComp.getProjectile().getMuzzleVelocity();
        projComp.getSimplePhysicsProvider().setVelocity(new Vector3d(0, -speed, 0));

        buffer.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("[FallingProjectile] spawned -> land (%.1f, %.1f, %.1f) speed=%.1f",
                landX, landY, landZ, speed);

        context.getState().state = InteractionState.Finished;
    }
}
